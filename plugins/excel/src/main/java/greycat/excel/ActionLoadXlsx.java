/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.excel;

import greycat.*;
import greycat.internal.task.TaskHelper;
import greycat.ml.profiling.Gaussian;
import greycat.plugin.Job;
import greycat.struct.Buffer;
import greycat.struct.Relation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
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

/**
 * Created by gnain on 27/02/17.
 */
class ActionLoadXlsx implements Action {
    private long _timeShiftConst = 3600 * 1000; //to convert hour to ms;
    private String _uri;
    private ZoneId _loaderZoneId = ZoneId.systemDefault();
    private FormulaEvaluator evaluator = null;

    private HashMap<String, Node> featuresMap = new HashMap<>();


    public ActionLoadXlsx(String uri, long timeshiftConst) {
        this._uri = uri;
        this._timeShiftConst = timeshiftConst;
    }

    @Override
    public void eval(TaskContext taskContext) {
        try {
            URI parsedUri = URI.create(taskContext.template(_uri));
            if (parsedUri.getScheme().equals("file")) {
                System.out.println("Opening Workbook");
                FileInputStream file = new FileInputStream(parsedUri.getPath());
                XSSFWorkbook workbook = new XSSFWorkbook(file);
                evaluator = workbook.getCreationHelper().createFormulaEvaluator();

                Sheet metaSheet = workbook.getSheet("META");
                if (metaSheet != null) {
                    loadMeta(taskContext, metaSheet);
                }
                loadSheets(taskContext, workbook, () -> {
                    taskContext.continueWith(taskContext.wrap(featuresMap.values().toArray()));
                });
            } else {
                throw new UnsupportedOperationException("Schema: " + parsedUri.getScheme() + " not yet supported. Tried to open file from: " + _uri);
            }
        } catch (Exception e) {
            taskContext.endTask(null, e);
        }
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

            Node newFeature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
            newFeature.set("tag_id", Type.STRING, featureId);
            newFeature.set("tag_from_meta", Type.BOOL, true);
            newFeature.set("tag_name", Type.STRING, featureName);
            if (currentRow.getCell(firstCol + 2) != null) {
                newFeature.set("tag_description", Type.STRING, currentRow.getCell(firstCol + 2).getStringCellValue());
            }
            if (currentRow.getCell(firstCol + 3) != null) {
                newFeature.set("tag_unit", Type.STRING, currentRow.getCell(firstCol + 3).getStringCellValue());
            }
            newFeature.set("tag_type", Type.STRING, currentRow.getCell(firstCol + 4).getStringCellValue().toLowerCase().trim());
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

        for (int r = metaSheet.getFirstRowNum() + 1; r <= lastRowNum; r++) {
            Row currentRow = metaSheet.getRow(r);
            if (currentRow == null) {
                continue;
            }
            int firstCol = currentRow.getFirstCellNum();
            String featureName = currentRow.getCell(firstCol + 1).getStringCellValue();
            Node newFeature = featuresMap.get(featureName);

            if (currentRow.getCell(firstCol + 9) != null) {
                XSSFCell cell = (XSSFCell) currentRow.getCell(firstCol + 9);
                CellType type = cell.getCellTypeEnum();
                if (type == CellType.STRING || type == CellType.FORMULA) {
                    String ts = cell.getStringCellValue();
                    Node tsnode = featuresMap.get(ts);
                    if (tsnode == null) {
                        throw new RuntimeException("Can't find this timeshift node: " + ts);
                    } else if (!tsnode.get("tag_type").equals("timeshift")) {
                        throw new RuntimeException("Tag " + tsnode.get("tag_type") + " is not marked as a type shift type");
                    } else {
                        Relation rel = (Relation) newFeature.getOrCreate("timeshift", Type.RELATION);
                        rel.addNode(tsnode);
                    }
                } else if (type == CellType.NUMERIC) {
                    //convert to ms
                    newFeature.set("timeshift", Type.LONG, currentRow.getCell(firstCol + 9).getNumericCellValue() * _timeShiftConst);
                }
            }
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
        System.out.println("Pre-Loading Sheet:" + sheet.getSheetName());
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
                final Cell cell = currentRow.getCell(c);
                CellValue currentCell = evaluator.evaluate(cell);

                if (currentCell == null) {
                    if ((c - 1) < featuresTrees.length) {
                        featuresTrees[c - 1].put(epochMillis, null);
                    }
                } else {
                    CellType ct = CellType.forInt(currentCell.getCellType());
                    if (ct == CellType.NUMERIC) {
                        featuresTrees[c - 1].put(epochMillis, currentCell.getNumberValue());
                    } else if (ct == CellType.STRING) {
                        if (currentCell.getStringValue().trim() != "") {
                            if (currentCell.getStringValue().trim().toLowerCase().equals("null")) {
                                featuresTrees[c - 1].put(epochMillis, null);
                            } else {
                                featuresTrees[c - 1].put(epochMillis, currentCell.getStringValue());
                            }
                        }
                    } else if (ct == CellType.BOOLEAN) {
                        featuresTrees[c - 1].put(epochMillis, currentCell.getBooleanValue());
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


    private void loadSheets(TaskContext taskContext, XSSFWorkbook workbook, Job callback) {

        int sheetNum = workbook.getNumberOfSheets();

        final DeferCounter countSheets = taskContext.graph().newCounter(sheetNum);
        countSheets.then(() -> callback.run());

        for (int i = 0; i < sheetNum; i++) {
            XSSFSheet currentSheet = workbook.getSheetAt(i);
            System.out.println((i+1) + "/" + sheetNum + " Loading Sheet:" + currentSheet.getSheetName());
            if (currentSheet.getSheetName().toLowerCase().trim().equals("meta")) {
                countSheets.count();
                continue;
            }

            Row headerRow = currentSheet.getRow(currentSheet.getFirstRowNum());
            if (headerRow == null) {
                taskContext.append("First row of sheet '" + currentSheet.getSheetName() + "' is empty. Sheet ignored.\n");
                countSheets.count();
                continue;
            }


            Map<String, TreeMap<Long, Object>> content = sheetPreload(taskContext, currentSheet);
            final DeferCounter countContent = taskContext.graph().newCounter(content.size());
            countContent.then(() -> countSheets.count());
            content.forEach((featureName, featureValues) -> {
                Node feature = featuresMap.get(featureName);
                if (feature == null) { // META nor loaded. Create feature, guess type while loading
                    feature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                    feature.set("tag_id", Type.INT, featuresMap.size());
                    feature.set("tag_name", Type.STRING, featureName);
                    feature.set("tag_from_meta", Type.BOOL, false);
                    featuresMap.put(featureName, feature);
                    insertValues(taskContext, feature, featureValues, true, () -> countContent.count());
                } else {
                    insertValues(taskContext, feature, featureValues, false, () -> countContent.count());
                }
            });

        }

    }

    private void insertValues(TaskContext taskContext, final Node feature, TreeMap<Long, Object> featureValues, boolean guessType, Job callback) {

        final byte type;
        if (guessType) {
            //TODO
            type = -1;

        } else {
            type = ((Integer) feature.get("value_type")).byteValue();
        }
        String tagType = ((String) feature.get("tag_type"));
        if (tagType.equals("timeshift")) {

            final Node valueNode = taskContext.graph().newNode(taskContext.world(), featureValues.firstKey());
            feature.addToRelation("value", valueNode);

            long firstkey = featureValues.firstKey();
            long firstshifted = firstkey + (long) ((double) featureValues.get(firstkey) * _timeShiftConst);

            final Node valueNodeShifted = taskContext.graph().newNode(taskContext.world(), firstshifted);
            feature.addToRelation("shiftedvalue", valueNodeShifted);

            DeferCounter defer = feature.graph().newCounter(featureValues.size() * 2);
            defer.then(() -> {

                if (valueNode != null) {
                    valueNode.free();
                }
                if (valueNodeShifted != null) {
                    valueNodeShifted.free();
                }

                if (type == Type.INT || type == Type.DOUBLE) {
                    double min = feature.getWithDefault(Gaussian.MIN, 0.0);
                    double max = feature.getWithDefault(Gaussian.MAX, 0.0);
                    if (max != min) {
                        featureValues.forEach((key, value) -> {
                            if (value != null) {
                                Gaussian.histogram(feature, min, max, (double) value);
                            }
                        });
                    }
                }
                callback.run();
            });
            featureValues.forEach((key, value) -> {
                long shift = (long) ((double) value * _timeShiftConst);
                setValueInTime(feature, valueNode, key, value, false, type, () -> defer.count());
                setValueInTime(feature, valueNodeShifted, key + shift, shift, false, Type.LONG, () -> defer.count());
            });

        } else {
            final Node valueNode = taskContext.graph().newNode(taskContext.world(), featureValues.firstKey());
            feature.addToRelation("value", valueNode);

            DeferCounter defer = feature.graph().newCounter(featureValues.size());
            defer.then(() -> {

                if (valueNode != null) {
                    valueNode.free();
                }

                if (type == Type.INT || type == Type.DOUBLE) {
                    double min = feature.getWithDefault(Gaussian.MIN, 0.0);
                    double max = feature.getWithDefault(Gaussian.MAX, 0.0);
                    if (max != min) {
                        featureValues.forEach((key, value) -> {
                            if (value != null) {
                                Gaussian.histogram(feature, min, max, (double) value);
                            }
                        });
                    }
                }
                callback.run();
            });
            featureValues.forEach((key, value) -> {
                setValueInTime(feature, valueNode, key, value, true, type, () -> defer.count());
            });
        }
    }

    private void setValueInTime(Node featureNode, Node valueNode, Long time, Object value, boolean profile, byte type, Job callback) {

        valueNode.travelInTime(time, jumped -> {
            try {
                if (jumped != null) {
                    if (value == null) {
                        jumped.set("value", type, null);
                    } else if (value instanceof String) {
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

                        if (profile && (type == Type.INT || type == Type.DOUBLE)) {
                            Gaussian.profile(featureNode, (double) value);
                        }
                    }
                }
            } catch (ClassCastException e) {
                e.printStackTrace();
            } finally {
                if (jumped != null) {
                    jumped.free();
                }
                callback.run();
            }
        });
    }

}
