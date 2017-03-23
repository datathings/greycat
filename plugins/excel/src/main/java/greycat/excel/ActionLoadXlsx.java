/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.excel;

import greycat.*;
import greycat.internal.task.TaskHelper;
import greycat.ml.profiling.Gaussian;
import greycat.ml.profiling.GaussianENode;
import greycat.struct.Buffer;
import greycat.struct.EGraph;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by gnain on 27/02/17.
 */
class ActionLoadXlsx implements Action {

    private String _uri;
    private ZoneId _loaderZoneId = ZoneId.systemDefault();

    private HashMap<String, Node> featuresMap = new HashMap<>();
    //private Node[] valuesArray;

    private ExecutorService es = Executors.newFixedThreadPool(3);

    public ActionLoadXlsx(String uri) {
        this._uri = uri;
    }

    @Override
    public void eval(TaskContext taskContext) {
        try {
            URI parsedUri = URI.create(taskContext.template(_uri));
            if (parsedUri.getScheme().equals("file")) {
                FileInputStream file = new FileInputStream(parsedUri.getPath());
                XSSFWorkbook workbook = new XSSFWorkbook(file);

                Sheet metaSheet = workbook.getSheet("META");
                if (metaSheet != null) {
                    loadMeta(taskContext, metaSheet);
                }
                loadSheets(taskContext, workbook);
                es.shutdown();
                while (!es.isTerminated()) {
                    es.awaitTermination(2, TimeUnit.SECONDS);
                }

            } else {
                throw new UnsupportedOperationException("Schema: " + parsedUri.getScheme() + " not yet supported. Tried to open file from: " + _uri);
            }
        } catch (Exception e) {
            taskContext.endTask(null, e);
        }
        taskContext.continueWith(taskContext.wrap(featuresMap.values().toArray()));

    }

    @Override
    public void serialize(Buffer buffer) {
        buffer.writeString(ExcelActions.LOAD_XSLX);
        buffer.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_uri, buffer, true);
        buffer.writeChar(Constants.TASK_PARAM_CLOSE);
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
            String featureId = "" + Math.round(currentRow.getCell(firstCol).getNumericCellValue());
            String featureName = currentRow.getCell(firstCol + 1).getStringCellValue();

            if (featuresMap.get(featureName) != null) {
                taskContext.append("Duplicate TAG name in META: " + featureName + "\n");
            }

            Node newFeature = taskContext.graph().newNode(taskContext.world(), Constants.BEGINNING_OF_TIME);
            newFeature.set("tag_id", Type.STRING, featureId);
            newFeature.set("tag_from_meta", Type.BOOL, true);
            newFeature.set("tag_name", Type.STRING, featureName);
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
        String bound = valueRange.split(";")[1].trim();
        return bound.substring(0, bound.length() - 1).replace(",", ".");
    }

    private int getIntLowerBound(String range) {
        return Integer.parseInt(getLowerBound(range));
    }

    private double getDoubleLowerBound(String range) {
        return Double.parseDouble(getLowerBound(range));
    }

    private String getLowerBound(String valueRange) {
        String bound = valueRange.split(";")[0].trim();
        return bound.substring(1).replace(",", ".");
    }

    private Map<String, TreeMap<Long, Object>> sheetPreload(TaskContext taskContext, XSSFSheet sheet) {
        HashMap<String, TreeMap<Long, Object>> sheetPreload = new HashMap<String, TreeMap<Long, Object>>();

        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            return sheetPreload;
        }

        //Load headers == features
        int lastCell = headerRow.getLastCellNum() - 1;
        TreeMap<Long, Object>[] featuresTrees = new TreeMap[lastCell - headerRow.getFirstCellNum()];
        for (int c = headerRow.getFirstCellNum() + 1; c <= lastCell; c++) {

            Cell currentCell = headerRow.getCell(c);
            if (currentCell == null) {
                continue;
            }
            if (CellType.forInt(currentCell.getCellType()) == CellType.BLANK) {
                continue;
            }
            String featureName;

            if (CellType.forInt(currentCell.getCellType()) == CellType.STRING) {
                featureName = currentCell.getStringCellValue();
            } else {
                featureName = sheet.getSheetName() + "_GEN_TAG_" + c;
            }
            featuresTrees[c - 1] = new TreeMap<Long, Object>();
            sheetPreload.put(featureName, featuresTrees[c - 1]);
        }

        //Load Values
        int lastRowNum = sheet.getLastRowNum();
        for (int r = sheet.getFirstRowNum() + 1; r <= lastRowNum; r++) {

            final Row currentRow = sheet.getRow(r);
            if (currentRow == null) {
                continue;
            }
            final Cell timeCell = currentRow.getCell(currentRow.getFirstCellNum());
            if (timeCell == null) {
                continue;
            }
            final Date timeCellValue = timeCell.getDateCellValue();
            if (timeCellValue == null) {
                continue;
            }

            ZonedDateTime rowTime = timeCellValue.toInstant().atZone(_loaderZoneId);
            long epochMillis = rowTime.toInstant().toEpochMilli();

            for (int c = currentRow.getFirstCellNum() + 1; c <= (currentRow.getLastCellNum() - 1); c++) {
                final Cell currentCell = currentRow.getCell(c);
                if (currentCell == null) {
                    if ((c - 1) < featuresTrees.length) {
                        featuresTrees[c - 1].put(epochMillis, null);
                    }
                } else {
                    CellType ct = CellType.forInt(currentCell.getCellType());
                    if (ct == CellType.NUMERIC) {
                        featuresTrees[c - 1].put(epochMillis, currentCell.getNumericCellValue());
                    } else if (ct == CellType.STRING) {
                        if (currentCell.getStringCellValue().trim().toLowerCase().equals("null")) {
                            featuresTrees[c - 1].put(epochMillis, null);
                        } else {
                            featuresTrees[c - 1].put(epochMillis, currentCell.getStringCellValue());
                        }
                    } else if (ct == CellType.BOOLEAN) {
                        if (currentCell.getStringCellValue().trim().toLowerCase().equals("null")) {
                            featuresTrees[c - 1].put(epochMillis, currentCell.getBooleanCellValue());
                        } else {
                            featuresTrees[c - 1].put(epochMillis, currentCell.getStringCellValue());
                        }
                    } else if (ct == CellType.BLANK || ct == CellType._NONE) {
                        if ((c - 1) < featuresTrees.length) {
                            featuresTrees[c - 1].put(epochMillis, null);
                        }
                    } else {
                        System.err.println("Unknown CellType:" + ct.name());
                    }
                }
            }
        }
        return sheetPreload;
    }


    private void loadSheets(TaskContext taskContext, XSSFWorkbook workbook) {

        int sheetNum = workbook.getNumberOfSheets();
        for (int i = 0; i < sheetNum; i++) {
            XSSFSheet currentSheet = workbook.getSheetAt(i);

            if (currentSheet.getSheetName().toLowerCase().trim().equals("meta")) {
                continue;
            }
            System.out.println("Loading Sheet:" + currentSheet.getSheetName());

            Row headerRow = currentSheet.getRow(currentSheet.getFirstRowNum());
            if (headerRow == null) {
                taskContext.append("First row of sheet '" + currentSheet.getSheetName() + "' is empty. Sheet ignored.\n");
                continue;
            }

            Map<String, TreeMap<Long, Object>> content = sheetPreload(taskContext, currentSheet);

            content.forEach((featureName, featureValues) -> {

                Node feature = featuresMap.get(featureName);
                if (feature == null) { // META nor loaded. Create feature, guess type while loading

                    feature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                    feature.set("tag_id", Type.INT, featuresMap.size());
                    feature.set("tag_name", Type.STRING, featureName);
                    feature.set("tag_from_meta", Type.BOOL, false);
                    featuresMap.put(featureName, feature);

                    insertValues(taskContext, feature, featureValues, true);

                } else {
                    insertValues(taskContext, feature, featureValues, false);
                }

            });



            /*
            int lastCell = headerRow.getLastCellNum();

            for (int c = headerRow.getFirstCellNum() + 1; c <= lastCell; c++) {

                final int cellNum = c;
                Cell currentCell = headerRow.getCell(c);
                if (currentCell == null) {
                    continue;
                }
                if (CellType.forInt(currentCell.getCellType()) == CellType.BLANK) {
                    continue;
                }
                String featureName;

                if (CellType.forInt(currentCell.getCellType()) == CellType.STRING) {
                    featureName = currentCell.getStringCellValue();
                } else {
                    featureName = currentSheet.getSheetName() + "_GEN_TAG_" + c;
                    taskContext.append("Wrong cell type " + CellType.forInt(currentCell.getCellType()) + " " + currentSheet.getSheetName() + ":" + currentCell.getAddress().formatAsString() + ". Generated: " + featureName + "\n");
                }
                Node feature = featuresMap.get(featureName);
                if (feature != null) {
                    try {
                        loadColumnWithType(taskContext, currentSheet, cellNum, feature, ((Integer) feature.get("value_type")).byteValue());
                        //guessColumnTypeAndLoad(taskContext, currentSheet, cellNum, feature);
                    } catch (Exception e) {
                        System.out.println("v");
                        e.printStackTrace();
                    }
                } else {

                    taskContext.append("Tag not listed in META: " + featureName + " from sheet " + currentSheet.getSheetName() + "\n");

                    Node newFeature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                    newFeature.set("tag_id", Type.INT, featuresMap.size());
                    newFeature.set("tag_name", Type.STRING, featureName);
                    newFeature.set("tag_from_meta", Type.BOOL, false);

                    featuresMap.put(featureName, newFeature);

                    guessColumnTypeAndLoad(taskContext, currentSheet, c, newFeature);
                }

            }*/
        }

    }


    private void insertValues(TaskContext taskContext, Node feature, TreeMap<Long, Object> featureValues, boolean guessType) {

        final byte type;
        if (guessType) {
            //TODO
            type = -1;
            /*
        byte colType = -1;
        int lastRowNum = currentSheet.getLastRowNum();
        for (int r = currentSheet.getFirstRowNum() + 1; r <= lastRowNum; r++) {
            try {
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
            } catch (Exception e) {
                System.out.println("rr");
            }
        }*/
        } else {
            type = ((Integer) feature.get("value_type")).byteValue();
        }


        final double[] min = {Double.MAX_VALUE};
        final double[] max = {Double.MIN_VALUE};
        final Node valueNode = taskContext.graph().newNode(taskContext.world(), featureValues.firstKey());
        feature.addToRelation("value", valueNode);

        GaussianENode gaussian = null;
        if(type==Type.DOUBLE||type==Type.INT){
            EGraph eg = (EGraph) feature.getOrCreate("profile", Type.EGRAPH);
            eg.setRoot(eg.newNode());
            gaussian=new GaussianENode(eg.root());
        }
        GaussianENode finalGaussian = gaussian;

        featureValues.forEach((key, value) -> {
            setValueInTime(valueNode, key, value, type, finalGaussian);
        });


        if (valueNode != null) {
            valueNode.free();
        }
        try {
            if (min[0] != Double.MAX_VALUE) {
                if (type == Type.LONG) {
                    feature.set("value_min", Type.LONG, Math.round(min[0]));
                } else {
                    feature.set("value_min", Type.DOUBLE, min[0]);
                }
            }
            if (max[0] != Double.MIN_VALUE) {
                if (type == Type.LONG) {
                    feature.set("value_max", Type.LONG, Math.round(max[0]));
                } else {
                    feature.set("value_max", Type.DOUBLE, max[0]);
                }
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

    }

    private void setValueInTime(Node featureNode, Long time, Object value, byte type, GaussianENode gaussianENode) {
        featureNode.travelInTime(time, jumped -> {
            try {
                if (jumped != null) {
                    if (value instanceof String) {
                        if (((String) value).trim().equals("")) {
                            jumped.set("value", type, null);
                        } else {
                            jumped.set("value", type, value);
                        }
                    } else {
                        if (type == Type.INT) {
                            jumped.set("value", type, ((Double) value).intValue());
                        } else {
                            jumped.set("value", type, value);
                        }
                        if(gaussianENode!=null){
                            gaussianENode.learn(new double[]{(Double) value});
                        }
                    }
                }
            } catch (ClassCastException e) {
                System.out.println();
            } finally {
                if (jumped != null) {
                    jumped.free();
                }
            }
        });
    }

}
