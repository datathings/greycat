/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.excel;

import greycat.*;
import greycat.internal.CoreNodeValue;
import greycat.internal.task.TaskHelper;
import greycat.plugin.Job;
import greycat.struct.Buffer;
import greycat.struct.Relation;
import greycat.struct.StringArray;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static greycat.excel.ExcelActions.LOAD_XSLX;

/**
 * Created by gnain on 27/02/17.
 */
class ActionLoadXlsx implements Action {
    private long _timeShiftConst = 3600 * 1000; //to convert hour to ms;
    private String _basePath;
    private String _uri;
    private String _timeShiftParam;
    private ZoneId _loaderZoneId = ZoneId.systemDefault();
    private FormulaEvaluator evaluator = null;

    private HashMap<String, Node> featuresMap = new HashMap<>();


    public ActionLoadXlsx(String basePath, String uri, String timeshiftConst) {
        this._basePath = basePath;
        this._uri = uri;
        this._timeShiftParam = timeshiftConst;
    }

    @Override
    public void eval(TaskContext taskContext) {
        try {
            this._timeShiftConst = Long.parseLong(taskContext.template(_timeShiftParam));
            String resolvedPath = taskContext.template(_uri);

            Path resolved = Paths.get(resolvedPath);
            URI parsedUri;
            if (resolved.toFile().exists()) {
                parsedUri = URI.create(resolved.toFile().toURI().toString());
            } else if (Paths.get(_basePath).resolve(resolved).toFile().exists()) {
                parsedUri = URI.create(Paths.get(_basePath).resolve(resolved).toFile().toURI().toString());
            } else {
                parsedUri = URI.create(resolvedPath);
            }

            if (parsedUri.getScheme().equals("file")) {
                System.out.println("Opening Workbook");

                taskContext.reportProgress(-1, "Opening Workbook");

                FileInputStream file = new FileInputStream(parsedUri.getPath());
                XSSFWorkbook workbook = new XSSFWorkbook(file);
                evaluator = workbook.getCreationHelper().createFormulaEvaluator();

                Sheet metaSheet = workbook.getSheet("META");
                if (metaSheet != null) {
                    taskContext.reportProgress(-1, "Loading META information");
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
        buffer.writeString(LOAD_XSLX);
        buffer.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_uri, buffer, true);
        buffer.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public String name() {
        return ExcelActions.LOAD_XSLX;
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

            if (featureName.trim().equals("")) {
                continue;
            }

            if (featuresMap.get(featureName) != null) {
                taskContext.append("Duplicate TAG name in META: " + featureName + "\n");
            }

            Node newFeature = taskContext.graph().newNode(taskContext.world(), Constants.BEGINNING_OF_TIME);
            newFeature.setTimeSensitivity(-1, 0);

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
                case "double":
                case "int": {
                    newFeature.set("value_type", Type.INT, Type.DOUBLE);
                    if (currentRow.getCell(firstCol + 7) != null) {
                        newFeature.set("value_min", Type.DOUBLE, getDoubleLowerBound(currentRow.getCell(firstCol + 7).getStringCellValue()));
                        newFeature.set("value_max", Type.DOUBLE, getDoubleUpperBound(currentRow.getCell(firstCol + 7).getStringCellValue()));
                    }
                }
                break;
                case "enum": {
                    newFeature.set("value_type", Type.INT, Type.DOUBLE);
                    if (currentRow.getCell(firstCol + 7) != null) {
                        String enumRange = currentRow.getCell(firstCol + 7).getStringCellValue();
                        String[] enumValues = enumRange.substring(1, enumRange.length() - 1).split(",");
                        if(enumValues.length == 0) {
                            throw new RuntimeException("Tag "+featureName+" is declared as enum, but possible values are not specified in cell " + currentRow.getCell(firstCol + 7).getAddress().toString() + " in sheet '" + metaSheet.getSheetName() + "'.");
                        }
                        StringArray valuesArray = (StringArray) newFeature.getOrCreate("enum_values", Type.STRING_ARRAY);
                        valuesArray.addAll(enumValues);
                        newFeature.set("value_min", Type.DOUBLE, 0d);
                        newFeature.set("value_max", Type.DOUBLE, (double)enumValues.length - 1);
                    } else {
                        throw new RuntimeException("Tag "+featureName+" is declared as enum, but possible values are not specified in cell " + CellReference.convertNumToColString(firstCol + 7) + (currentRow.getRowNum()+1) + " in sheet '" + metaSheet.getSheetName() + "'.");
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
                        Relation rel = (Relation) newFeature.getOrCreate("timeshiftRel", Type.RELATION);
                        rel.addNode(tsnode);
                    }
                } else if (type == CellType.NUMERIC) {
                    //convert to ms
                    newFeature.set("timeshift", Type.LONG, (long) (currentRow.getCell(firstCol + 9).getNumericCellValue() * _timeShiftConst));
                }
            }
        }

    }



    private double getDoubleUpperBound(String range) {
        return Double.parseDouble(getUpperBound(range));
    }

    private String getUpperBound(String valueRange) {
        String bound = valueRange.split(";")[1].trim();
        return bound.substring(0, bound.length() - 1).replace(",", ".");
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
            String featureName = null;

            if (CellType.forInt(currentCell.getCellType()) == CellType.STRING) {
                featureName = currentCell.getStringCellValue();
            }
            if (featureName == null || featureName.trim().equals("")) {
                throw new RuntimeException("Could not read tag_name from cell in first row of column n°" + (c + 1) + " from sheet " + sheet.getSheetName() + ". Empty or not of type String. Please check.");
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
            Date timeCellValue = null;
            try {
                timeCellValue = timeCell.getDateCellValue();
            } catch (IllegalStateException e) {
                throw new RuntimeException("Could not read a timestamp from cell " + timeCell.getAddress().toString() + " in sheet '" + sheet.getSheetName() + "'.", e);
            }

            if (timeCellValue == null) {
                continue;
            }

            ZonedDateTime rowTime = timeCellValue.toInstant().atZone(_loaderZoneId);
            long epochMillis = rowTime.toInstant().toEpochMilli();

            for (int c = currentRow.getFirstCellNum() + 1; c <= (currentRow.getLastCellNum() - 1); c++) {
                final Cell cell = currentRow.getCell(c);
                try {
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
                } catch (IllegalStateException e) {
                    throw new RuntimeException("Could not read the content of cell " + cell.getAddress().toString() + " in sheet '" + sheet.getSheetName() + "'.", e);
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
            System.out.println((i + 1) + "/" + sheetNum + " Loading Sheet:" + currentSheet.getSheetName());
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

            taskContext.reportProgress((i * 2.)/(sheetNum * 2), "Data pre-load from " + currentSheet.getSheetName());
            Map<String, TreeMap<Long, Object>> content = sheetPreload(taskContext, currentSheet);
            taskContext.reportProgress(((i * 2.) + 1)/(sheetNum * 2), "Data insertion from " + currentSheet.getSheetName());
            final DeferCounter countContent = taskContext.graph().newCounter(content.size());
            countContent.then(() -> countSheets.count());
            content.forEach((featureName, featureValues) -> {
                Node feature = featuresMap.get(featureName);
                if (feature == null) { // META nor loaded. Create feature, guess type while loading
                    throw new RuntimeException("Feature '" + featureName + "' from sheet " + currentSheet.getSheetName() + " not declared in META sheet. Abort loading.");
                    /* TYPE INFERENCE NOT SUPPORTED
                    feature = taskContext.graph().newNode(taskContext.world(), taskContext.time());
                    feature.set("tag_id", Type.INT, featuresMap.size());
                    feature.set("tag_name", Type.STRING, featureName);
                    feature.set("tag_from_meta", Type.BOOL, false);
                    featuresMap.put(featureName, feature);
                    insertValues(taskContext, feature, featureValues, true, () -> countContent.count());
                    */
                } else {
                    insertValues(taskContext, feature, featureValues, () -> countContent.count());
                }
            });

        }

    }

    private void insertValues(TaskContext taskContext, final Node feature, TreeMap<Long, Object> featureValues, Job callback) {

        final byte type = ((Integer) feature.get("value_type")).byteValue();
        String tagType = ((String) feature.get("tag_type"));
        if (tagType != null && tagType.equals("timeshift")) {

            long firstkey = featureValues.firstKey();
            long firstshifted = firstkey + (long) ((double) featureValues.get(firstkey) * _timeShiftConst);


            //todo reactivate value node once null are accepted!
            //final NodeValue valueNode = (NodeValue) taskContext.graph().newTypedNode(taskContext.world(), featureValues.firstKey(), CoreNodeValue.NAME);
            //final NodeValue valueNodeShifted = (NodeValue) taskContext.graph().newTypedNode(taskContext.world(), firstshifted, CoreNodeValue.NAME);

            final Node valueNode = taskContext.graph().newNode(taskContext.world(), featureValues.firstKey());
            final Node valueNodeShifted = taskContext.graph().newNode(taskContext.world(), firstshifted);

            feature.addToRelation("value", valueNode);


            feature.addToRelation("shiftedvalue", valueNodeShifted);

            DeferCounter defer = feature.graph().newCounter(featureValues.size() * 2);
            defer.then(() -> {

                if (valueNode != null) {
                    valueNode.free();
                }
                if (valueNodeShifted != null) {
                    valueNodeShifted.free();
                }

                callback.run();
            });
            featureValues.forEach((key, value) -> {
                long shift = (long) ((double) value * _timeShiftConst);
                setValueInTime(feature, valueNode, key, value, type, false, null, () -> defer.count());
                setValueInTime(feature, valueNodeShifted, key + shift, shift, (byte)Type.LONG, false, null, () -> defer.count());
            });

        } else {
            final NodeValue valueNode = (NodeValue) taskContext.graph().newTypedNode(taskContext.world(), featureValues.firstKey(), CoreNodeValue.NAME);
            feature.addToRelation("value", valueNode);

            DeferCounter defer = feature.graph().newCounter(featureValues.size());
            defer.then(() -> {

                if (valueNode != null) {
                    valueNode.free();
                }

                callback.run();
            });

            final boolean isEnum;
            Map<String, Double> enumValues = new HashMap<>();
            if (feature.get("enum_values") != null) {
                StringArray values = (StringArray) feature.get("enum_values");
                for (int i = 0; i < values.size(); i++) {
                    enumValues.put(values.get(i), (double) i);
                }
                isEnum = true;
            } else {
                isEnum = false;
            }

            featureValues.forEach((key, value) -> {
                setValueInTime(feature, valueNode, key, value, type, isEnum, enumValues, () -> defer.count());
            });
        }
    }

    private void setValueInTime(Node featureNode, Node valueNode, Long time, Object value, byte type, boolean isEnum, Map<String, Double> enumValues, Job callback) {

        valueNode.travelInTime(time, jumped -> {
            try {
                if (jumped != null) {
                    if (value == null) {
                        jumped.set("value", type, null);
                    } else if (value instanceof String) {
                        if (isEnum) {
                            if (((String) value).trim().equals("") || !enumValues.containsKey(((String) value).trim())) {
                                jumped.set("value", type, -1d);
                            } else {
                                jumped.set("value", type, enumValues.get(((String) value).trim()));
                            }
                        } else {
                            if (((String) value).trim().equals("")) {
                                jumped.set("value", type, null);
                            } else {
                                jumped.set("value", type, value);
                            }
                        }

                    } else {
                        if (type == Type.INT) {
                            jumped.set("value", type, ((Double) value).intValue());
                        } else {
                            if (type < 0) {
                                System.out.println("type: " + type + " at: " + featureNode.get("tag_name"));
                            }
                            jumped.set("value", type, value);
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
