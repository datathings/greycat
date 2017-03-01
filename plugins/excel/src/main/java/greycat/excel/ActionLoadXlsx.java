package greycat.excel;

import greycat.Action;
import greycat.Node;
import greycat.TaskContext;
import greycat.Type;
import greycat.struct.Buffer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by gnain on 27/02/17.
 */
public class ActionLoadXlsx implements Action {

    private String _uri;
    private ZoneId _loaderZoneId = ZoneId.systemDefault();

    private HashMap<String, Node> featuresMap = new HashMap<>();
    //private Node[] valuesArray;

    public ActionLoadXlsx(String uri) {
        this._uri = uri;
    }

    @Override
    public void eval(TaskContext taskContext) {
        URI parsedUri = URI.create(taskContext.template(_uri));
        if (parsedUri.getScheme().equals("file")) {
            try {
                FileInputStream file = new FileInputStream(parsedUri.getPath());
                XSSFWorkbook workbook = new XSSFWorkbook(file);

                Node fileNode = taskContext.resultAsNodes().get(0);
                System.out.println("ZoneId:" + _loaderZoneId);

                Sheet metaSheet = workbook.getSheet("META");
                if (metaSheet != null) {
                    loadMeta(taskContext, metaSheet);
                    loadSheetsWithMeta(taskContext, workbook);
                } else {
                    loadSheets(taskContext, workbook);
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            throw new UnsupportedOperationException("Schema: " + parsedUri.getScheme() + " not yet supported. Tried to open file from: " + _uri);
        }
        taskContext.continueWith(taskContext.wrap(featuresMap.values().toArray()));

    }

    @Override
    public void serialize(Buffer buffer) {

    }

    private void loadMeta(TaskContext taskContext, Sheet metaSheet) {
        System.out.println("Load from META:" + metaSheet);

        int lastRowNum = metaSheet.getLastRowNum();
        for (int r = metaSheet.getFirstRowNum() + 1; r <= lastRowNum; r++) {

            Row currentRow = metaSheet.getRow(r);
            if (currentRow == null) {
                continue;
            }

            int firstCol = currentRow.getFirstCellNum();
            String featureName = "" + Math.round(currentRow.getCell(firstCol).getNumericCellValue());

            Node newFeature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
            newFeature.set("tag_id", Type.STRING, featureName);
            newFeature.set("tag_from_meta", Type.BOOL, true);
            newFeature.set("tag_name", Type.STRING, currentRow.getCell(firstCol + 1).getStringCellValue());
            if (currentRow.getCell(firstCol + 2) != null) {
                newFeature.set("tag_description", Type.STRING, currentRow.getCell(firstCol + 2).getStringCellValue());
            }
            if (currentRow.getCell(firstCol + 3) != null) {
                newFeature.set("tag_unit", Type.STRING, currentRow.getCell(firstCol + 3).getStringCellValue());
            }
            newFeature.set("tag_type", Type.STRING, currentRow.getCell(firstCol + 4).getStringCellValue());
            if (currentRow.getCell(firstCol + 5) != null) {
                newFeature.set("value_precision", Type.DOUBLE, currentRow.getCell(firstCol + 5).getNumericCellValue());
            }
            String valueType = currentRow.getCell(firstCol + 6).getStringCellValue();
            switch (valueType.trim().toLowerCase()) {
                case "double": {
                    newFeature.set("value_type", Type.INT, Type.DOUBLE);
                    if (currentRow.getCell(firstCol + 7) != null) {
                        newFeature.set("value_min", Type.DOUBLE, getDoubleLowerBound(currentRow.getCell(firstCol + 7).getStringCellValue()));
                        newFeature.set("value_max", Type.DOUBLE, getDoubleUpperBound(currentRow.getCell(firstCol + 7).getStringCellValue()));
                    }
                }
                break;
                case "int": {
                    newFeature.set("value_type", Type.INT, Type.INT);
                    if (currentRow.getCell(firstCol + 7) != null) {
                        newFeature.set("value_min", Type.INT, getIntLowerBound(currentRow.getCell(firstCol + 7).getStringCellValue()));
                        newFeature.set("value_max", Type.INT, getIntUpperBound(currentRow.getCell(firstCol + 7).getStringCellValue()));
                    }
                }
                break;
                case "enum": {
                    newFeature.set("value_type", Type.INT, Type.STRING);
                    if (currentRow.getCell(firstCol + 7) != null) {
                        newFeature.set("value_range", Type.STRING, currentRow.getCell(firstCol + 7).getStringCellValue());
                    }
                }
                break;
                default: {

                }
            }
            if (currentRow.getCell(firstCol + 8) != null) {
                newFeature.set("interpolation", Type.BOOL, currentRow.getCell(firstCol + 8).getBooleanCellValue());
            } else {
                newFeature.set("interpolation", Type.BOOL, false);
            }

            Node newValuesNode = taskContext.graph().newNode(taskContext.world(), taskContext.time());
            newFeature.addToRelation("value", newValuesNode);

            featuresMap.put("" + featureName, newFeature);

        }


    }

    private int getIntUpperBound(String range) {
        return Integer.parseInt(getUpperBound(range));
    }

    private double getDoubleUpperBound(String range) {
        return Double.parseDouble(getUpperBound(range));
    }

    private String getUpperBound(String valueRange) {
        String bound = valueRange.split(",")[1].trim();
        return bound.substring(0, bound.length() - 1);
    }

    private int getIntLowerBound(String range) {
        return Integer.parseInt(getLowerBound(range));
    }

    private double getDoubleLowerBound(String range) {
        return Double.parseDouble(getLowerBound(range));
    }

    private String getLowerBound(String valueRange) {
        String bound = valueRange.split(",")[0].trim();
        return bound.substring(1);
    }


    private void loadSheetsWithMeta(TaskContext taskContext, XSSFWorkbook workbook) {

        int sheetNum = workbook.getNumberOfSheets();
        for (int i = 0; i < sheetNum; i++) {
            XSSFSheet currentSheet = workbook.getSheetAt(i);

            if (currentSheet.getSheetName().toLowerCase().trim().equals("META")) {
                continue;
            }

            Row headerRow = currentSheet.getRow(currentSheet.getFirstRowNum());
            if (headerRow == null) {
                taskContext.append("First row of sheet '" + currentSheet.getSheetName() + "' is empty. Sheet ignored.\n");
                continue;
            }
            int lastCell = headerRow.getLastCellNum();
            //valuesArray = new Node[lastCell];
            for (int c = headerRow.getFirstCellNum() + 1; c <= lastCell; c++) {

                Cell currentCell = headerRow.getCell(c);
                if (currentCell == null) {
                    continue;
                }
                String featureName;
                if (currentCell != null && CellType.forInt(currentCell.getCellType()) == CellType.STRING) {
                    featureName = currentCell.getStringCellValue();
                } else {
                    featureName = currentSheet.getSheetName() + "_GEN_TAG_" + c;
                    taskContext.append("Wrong cell type " + CellType.forInt(currentCell.getCellType()) + " " + currentSheet.getSheetName() + ":" + currentCell.getAddress().formatAsString() + ". Generated: " + featureName + "\n");
                }
                Node feature = featuresMap.get(featureName);
                if (feature != null) {

                } else {
                    Node newFeature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                    newFeature.set("tag_id", Type.INT, featuresMap.size());
                    newFeature.set("tag_name", Type.STRING, featureName);
                    newFeature.set("tag_from_meta", Type.BOOL, false);

                    Node newValuesNode = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                    newFeature.addToRelation("value", newValuesNode);

                    featuresMap.put(featureName, newFeature);

                    loadColumn(taskContext, currentSheet, c, newFeature, newValuesNode);
                }

            }
        }

    }

    private void loadSheets(TaskContext taskContext, XSSFWorkbook workbook) {
        int sheetNum = workbook.getNumberOfSheets();
        for (int i = 0; i < sheetNum; i++) {
            XSSFSheet currentSheet = workbook.getSheetAt(i);

            Row headerRow = currentSheet.getRow(currentSheet.getFirstRowNum());
            if (headerRow == null) {
                taskContext.append("First row of sheet '" + currentSheet.getSheetName() + "' is empty. Sheet ignored.\n");
                continue;
            }
            int lastCell = headerRow.getLastCellNum();
            for (int c = headerRow.getFirstCellNum() + 1; c <= lastCell; c++) {

                Cell currentCell = headerRow.getCell(c);
                if (currentCell == null) {
                    continue;
                }
                String featureName;

                if (currentCell != null && CellType.forInt(currentCell.getCellType()) == CellType.STRING) {
                    featureName = currentCell.getStringCellValue();
                } else {
                    featureName = currentSheet.getSheetName() + "_GEN_TAG_" + c;
                    taskContext.append("Wrong cell type " + CellType.forInt(currentCell.getCellType()) + " " + currentSheet.getSheetName() + ":" + currentCell.getAddress().formatAsString() + ". Generated: " + featureName + "\n");
                }

                Node newFeature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                newFeature.set("tag_id", Type.INT, featuresMap.size());
                newFeature.set("tag_name", Type.STRING, featureName);
                newFeature.set("tag_from_meta", Type.BOOL, false);

                Node newValuesNode = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                newFeature.addToRelation("value", newValuesNode);

                featuresMap.put(featureName, newFeature);

                loadColumn(taskContext, currentSheet, c, newFeature, newValuesNode);
            }
        }
    }


    private void loadColumn(TaskContext taskContext, XSSFSheet currentSheet, int colId, Node feature, final Node valueNode) {

        //Guess type
        byte colType = -1;
        int lastRowNum = currentSheet.getLastRowNum();
        for (int r = currentSheet.getFirstRowNum() + 1; r <= lastRowNum; r++) {
            Row currentRow = currentSheet.getRow(r);
            if (currentRow == null) {
                continue;
            }
            Cell currentCell = currentRow.getCell(colId);
            if (currentCell == null) {
                continue;
            }

            CellType ct = CellType.forInt(currentCell.getCellType());
            if (ct == CellType.STRING) {
                if (colType == -1) {
                    colType = Type.STRING;
                } else if (colType != Type.STRING) {
                    if (!currentCell.getStringCellValue().trim().toLowerCase().equals("null")) {
                        taskContext.append("Inconsistent types in column: " + currentCell.getAddress().formatAsString() + "\n");
                        return;
                    } else {
                        continue;
                    }
                }
            } else if (ct == CellType.BOOLEAN) {
                if (colType == -1) {
                    colType = Type.BOOL;
                } else if (colType != Type.BOOL) {
                    taskContext.append("Inconsistent types in column: " + currentCell.getAddress().formatAsString() + "\n");
                    return;
                }
            } else if (ct == CellType.NUMERIC) {
                boolean isDecimal = (currentCell.getNumericCellValue() % 1 != 0);
                if (colType == -1) {
                    colType = (isDecimal ? Type.DOUBLE : Type.LONG);
                } else if (isDecimal) {
                    colType = Type.DOUBLE;
                }
            } else if (ct == CellType.BLANK) {
                //ignore
            } else {
                taskContext.append("Could not find appropriate type for cell: " + currentCell.getAddress().formatAsString() + " " + ct + " \n");
                return;
            }
        }


        //load
        if (colType != -1) {
            feature.set("value_type", Type.STRING, Type.typeName(colType));
            final byte finalType = colType;

            loadColumnWithType(taskContext, currentSheet, colId, feature, valueNode, colType);
        } else {
            taskContext.append("Could not load values for column '" + colId + "' type is undefined.\n");
        }

    }

    private void loadColumnWithType(TaskContext taskContext, XSSFSheet currentSheet, int colId, Node feature, final Node valueNode, final byte finalType) {
        final double[] min = {Double.MAX_VALUE};
        final double[] max = {Double.MIN_VALUE};


        int lastRowNum = currentSheet.getLastRowNum();
        for (int r = currentSheet.getFirstRowNum() + 1; r <= lastRowNum; r++) {

            Row currentRow = currentSheet.getRow(r);
            if (currentRow == null) {
                continue;
            }
            Cell timeCell = currentRow.getCell(currentRow.getFirstCellNum());
            if (timeCell == null) {
                continue;
            }
            Date timeCellValue = timeCell.getDateCellValue();
            if (timeCellValue == null) {
                continue;
            }

            ZonedDateTime rowTime = timeCellValue.toInstant().atZone(_loaderZoneId);
            Cell currentCell = currentRow.getCell(colId);
            if (currentCell == null) {
                continue;
            }
            CellType ct = CellType.forInt(currentCell.getCellType());
            if (finalType == Type.DOUBLE && ct != CellType.NUMERIC) {
                if (ct == CellType.STRING && currentCell.getStringCellValue().trim().toLowerCase().equals("null")) {
                    continue;
                }
                if (ct == CellType.BLANK) {
                    continue;
                }
                taskContext.append("Ignored value of cell: " + currentSheet.getSheetName() + "!" + currentCell.getAddress() + "\n");
                continue;
            } else {
                valueNode.travelInTime(rowTime.toInstant().toEpochMilli(), jumped -> {
                    try {
                        if (finalType == Type.STRING) {
                            jumped.set("value", finalType, currentCell.getStringCellValue());
                        } else if (finalType == Type.LONG || finalType == Type.DOUBLE) {
                            double value = currentCell.getNumericCellValue();
                            if (value > max[0]) {
                                max[0] = value;
                            }
                            if (value < min[0]) {
                                min[0] = value;
                            }
                            if (finalType == Type.LONG) {
                                jumped.set("value", finalType, Long.valueOf("" + Math.round(value)));
                            } else {
                                jumped.set("value", finalType, value);
                            }
                        } else if (finalType == Type.BOOL) {
                            jumped.set("value", finalType, currentCell.getBooleanCellValue());
                        }
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    } finally {
                        jumped.free();
                    }
                });
            }
        }
        if (min[0] != Double.MAX_VALUE) {
            if (finalType == Type.LONG) {
                feature.set("value_min", Type.LONG, min[0] / 1);
            } else {
                feature.set("value_min", Type.DOUBLE, min[0]);
            }
        }
        if (max[0] != Double.MIN_VALUE) {
            if (finalType == Type.LONG) {
                feature.set("value_max", Type.LONG, max[0] / 1);
            } else {
                feature.set("value_max", Type.DOUBLE, max[0]);
            }
        }
    }


}
