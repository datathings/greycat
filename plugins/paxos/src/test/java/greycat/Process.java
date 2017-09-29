package greycat;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

import de.erichseifert.gral.data.DataSource;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.plots.BoxPlot;
import de.erichseifert.gral.plots.BoxPlot.BoxWhiskerRenderer;
import de.erichseifert.gral.plots.XYPlot.XYNavigationDirection;
import de.erichseifert.gral.plots.axes.Axis;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.axes.LogarithmicRenderer2D;
import de.erichseifert.gral.plots.colors.LinearGradient;
import de.erichseifert.gral.plots.colors.ScaledContinuousColorMapper;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.DataUtils;
import de.erichseifert.gral.util.GraphicsUtils;
import de.erichseifert.gral.graphics.Insets2D;

import javax.swing.*;

public class Process {

    public static void main(String[] args) {
        HashMap<Integer, List<Integer>> fusion = new HashMap<Integer, List<Integer>>();
        readCSV("/Users/duke/Documents/datathings/greycat/plugins/paxos/src/test/java/greycat/FusionResults.csv", fusion);

        HashMap<Integer, List<Integer>> gdlock = new HashMap<Integer, List<Integer>>();
        readCSV("/Users/duke/Documents/datathings/greycat/plugins/paxos/src/test/java/greycat/gdlock/Result.csv", gdlock);

        HashMap<Integer, List<Integer>> raft = new HashMap<Integer, List<Integer>>();
        readCSV("/Users/duke/Documents/datathings/greycat/plugins/paxos/src/test/java/greycat/RaftResults.csv", raft);

        HashMap<Integer, List<Integer>> raft7 = new HashMap<Integer, List<Integer>>();
        readCSV("/Users/duke/Documents/datathings/greycat/plugins/paxos/src/test/java/greycat/Raft6.csv", raft7);

        List<Integer> speedFusion = computeSpeed(fusion);
        List<Integer> speedDlock = computeSpeed(gdlock);
        List<Integer> speedRaft = computeSpeed(raft);
        List<Integer> speedRaft7 = computeSpeed(raft7);

        System.out.println("fusionAvg=" + computeAvg(speedFusion));
        System.out.println("dlockAvg=" + computeAvg(speedDlock));
        System.out.println("raftAvg=" + computeAvg(speedRaft));
        System.out.println("raft7Avg=" + computeAvg(speedRaft7));
        /*
        StringBuilder buffer = new StringBuilder();
        print("fusion", speedFusion, buffer);
        print("raft", speedRaft, buffer);

        try {
            PrintWriter writer = new PrintWriter("/Users/duke/Documents/datathings/greycat/plugins/paxos/src/test/java/greycat/result.csv", "UTF-8");
            writer.write(buffer.toString().toCharArray());
            writer.close();
        } catch (IOException e) {
            // do something
        }*/
        //SimpleBoxPlot(speedFusion, speedRaft, speedRaft6);

        SimpleBoxPlot(new String[]{"LockFree_Merge", "CentralServer_Lock","RaftConsensus_2_Peers","RaftConsensus_7_Peers"},speedFusion, speedDlock, speedRaft, speedRaft7);

    }

    public static void print(String name, List<Integer> elems, StringBuilder buffer) {
        buffer.append(name);
        buffer.append("\n");
        buffer.append("[");
        for (int i = 0; i < elems.size(); i++) {
            if (i != 0) {
                buffer.append(",");
            }
            buffer.append(elems);
        }
        buffer.append("]");
        buffer.append("\n");
    }

    public static int computeAvg(List<Integer> store) {
        int val = 0;
        for (int i = 0; i < store.size(); i++) {
            val += store.get(i);
        }
        return val / store.size();
    }

    public static List<Integer> computeSpeed(Map<Integer, List<Integer>> store) {
        Integer[] sizes = store.keySet().toArray(new Integer[store.size()]);
        Arrays.sort(sizes);
        List<Integer> previous = null;
        List<Integer> speed = new ArrayList<Integer>();
        for (int i = 0; i < sizes.length; i++) {
            int scale = sizes[i];
            if (scale > 100000) {
                return speed;
            }
            List<Integer> times = store.get(scale);
            if (previous != null) {
                if (times.size() < previous.size()) {
                    throw new RuntimeException("Bad Size!!!");
                }
                for (int j = 0; j < previous.size(); j++) {
                    double delay_1000k = times.get(j) - previous.get(j);
                    double gspeed = Config.saveEvery / (delay_1000k / 1000);
                    speed.add((int) gspeed);
                }
            }
            previous = times;
        }
        return speed;
    }

    public static void readCSV(String file, Map<Integer, List<Integer>> store) {
        BufferedReader br = null;
        String line;
        String cvsSplitBy = ";";
        try {
            br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                String[] cells = line.split(cvsSplitBy);
                Integer key = Integer.parseInt(cells[0]);
                List<Integer> seq = store.get(key);
                if (seq == null) {
                    seq = new ArrayList<Integer>();
                    store.put(key, seq);
                }
                seq.add(Integer.parseInt(cells[1]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final Random random = new Random();
    protected static final Color COLOR1 = new Color(55, 170, 200);
    protected static final Color COLOR2 = new Color(200, 80, 75);

    public static void SimpleBoxPlot(String[] names, List<Integer>... datasets) {

        // Create example data
        DataTable data = new DataTable(datasets.length, Integer.class);

        int maxSize = 0;
        for (int i = 0; i < datasets.length; i++) {
            if (maxSize < datasets[i].size()) {
                maxSize = datasets[i].size();
            }
        }

        for (int i = 0; i < maxSize; i++) {
            List<Integer> raw = new ArrayList<Integer>(datasets.length);
            for (int j = 0; j < datasets.length; j++) {
                raw.add(datasets[j].get(i % datasets[j].size()));
            }
            data.add(raw);
        }

        // Create new box-and-whisker plot
        DataSource boxData = BoxPlot.createBoxData(data);
        BoxPlot plot = new BoxPlot(boxData);

        // Format plot
        plot.autoscaleAxis(BoxPlot.AXIS_Y);

        plot.setInsets(new Insets2D.Double(20.0, 70.0, 40.0, 20.0));

        // Format axes

        Double[] ids = new Double[datasets.length];
        for (int i = 0; i < datasets.length; i++) {
            ids[i] = i + 1.0;
        }
        plot.getAxisRenderer(BoxPlot.AXIS_X).setCustomTicks(
                DataUtils.map(
                        ids,
                        names
                )
        );
        Axis axisY = new Axis();

        int max = 0;
        for (int i = 0; i < datasets.length; i++) {
            List<Integer> dataset = datasets[i];
            for (int j = 0; j < dataset.size(); j++) {
                int val = dataset.get(j);
                if (max < val) {
                    max = val;
                }
            }
        }

        axisY.setRange(0.0, max);
        AxisRenderer axisRendererY = new LogarithmicRenderer2D();
        axisRendererY.setTickSpacing(2.6);
        plot.setAxis(BoxPlot.AXIS_Y, axisY);
        plot.setAxisRenderer(BoxPlot.AXIS_Y, axisRendererY);

        // Format boxes
        Stroke stroke = new BasicStroke(2f);
        ScaledContinuousColorMapper colors = new LinearGradient(GraphicsUtils.deriveBrighter(COLOR1), Color.WHITE);
        colors.setRange(1.0, 3.0);
        BoxWhiskerRenderer pointRenderer = (BoxWhiskerRenderer) plot.getPointRenderers(boxData).get(0);
        pointRenderer.setWhiskerStroke(stroke);
        pointRenderer.setBoxBorderStroke(stroke);
        pointRenderer.setBoxBackground(colors);
        pointRenderer.setBoxBorderColor(COLOR1);
        pointRenderer.setWhiskerColor(COLOR1);
        pointRenderer.setCenterBarColor(COLOR1);
        plot.getNavigator().setDirection(XYNavigationDirection.VERTICAL);
        // Add plot to Swing component
        InteractivePanel panel = new InteractivePanel(plot);
        JFrame frame = new JFrame("Hello");
        panel.setPreferredSize(new Dimension(1200, 400));
        panel.setSize(new Dimension(1200, 5-400));
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(1200, 400));
        frame.setSize(new Dimension(1200, 400));
        //frame.pack();
        frame.setVisible(true);
    }

}
