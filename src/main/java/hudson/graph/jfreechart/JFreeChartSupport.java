/*
 * The MIT License
 *
 * Copyright 2011 Winston.Prakash@oracle.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.graph.jfreechart;

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;

import hudson.Extension;
import hudson.util.graph.ChartLabel;
import hudson.util.graph.DataSet;
import hudson.util.graph.Graph;
import hudson.util.graph.GraphSupport;
import hudson.util.graph.GraphSupportDescriptor;
import hudson.util.graph.MultiStageTimeSeries;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Paint;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * JFreeChart Support for Hudson Graph Support
 * @author Winston Prakash
 */
public class JFreeChartSupport extends GraphSupport {

    private static final Logger LOGGER = Logger.getLogger(JFreeChartSupport.class.getName());
    private JFreeChart jFreeChart;
    private ChartRenderingInfo info = new ChartRenderingInfo();
    private DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    private String chartTitle;
    private String xAxisLabel;
    private String yAxisLabel;
    public List<MultiStageTimeSeries> multiStageTimeSeries;

    private int chartType = 1; // 1 - StackedArea, 2 - Line

    @DataBoundConstructor
    public JFreeChartSupport() {
    }


    public void setChartType(int chartType) {
        this.chartType = chartType;
    }

    public void setTitle(String title) {
        chartTitle = title;
    }

    @Override
    public void setXAxisLabel(String xLabel) {
        xAxisLabel = xLabel;
    }

    @Override
    public void setYAxisLabel(String yLabel) {
        yAxisLabel = yLabel;
    }

    public void setMultiStageTimeSeries(List<MultiStageTimeSeries> multiStageTimeSeries) {
        this.multiStageTimeSeries = multiStageTimeSeries;
    }

    public void createChart() {

        if (chartType == Graph.TYPE_STACKED_AREA) {
            jFreeChart = ChartFactory.createStackedAreaChart(null, // chart
                    chartTitle, // // title 
                    xAxisLabel, // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    false, // include legend
                    true, // tooltips
                    false // urls
                    );
        } else if (chartType == Graph.TYPE_LINE) {
            jFreeChart = ChartFactory.createLineChart(null, // chart title
                    chartTitle, // // title 
                    xAxisLabel, // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    true, // include legend
                    true, // tooltips
                    false // urls
                    );
        }

        jFreeChart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = jFreeChart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);
        // plot.setDomainGridlinesVisible(true);
        // plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        if (chartType == Graph.TYPE_LINE) {
            final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setBaseStroke(new BasicStroke(3));

            if (multiStageTimeSeries != null) {
                for (int i = 0; i < multiStageTimeSeries.size(); i++) {
                    renderer.setSeriesPaint(i, multiStageTimeSeries.get(i).color);
                }
            }
        }


        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        Utils.adjustChebyshev(dataset, rangeAxis);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        if (chartType == Graph.TYPE_STACKED_AREA) {
            StackedAreaRenderer ar = new StackedAreaRenderer2() {

                @Override
                public Paint getItemPaint(int row, int column) {
                    ChartLabel key = (ChartLabel) dataset.getColumnKey(column);
                    return key.getColor(row, column);
                }

                @Override
                public String generateURL(CategoryDataset dataset, int row, int column) {
                    ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                    return label.getLink(row, column);
                }

                @Override
                public String generateToolTip(CategoryDataset dataset, int row,
                        int column) {
                    ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                    return label.getToolTip(row, column);
                }
            };
            plot.setRenderer(ar);
        }

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));
    }

    @Override
    public BufferedImage render(int width, int height) {
        createChart();
        return jFreeChart.createBufferedImage(width, height, info);
    }

    @Override
    public void setData(DataSet data) {

        TreeSet rowSet = new TreeSet(data.getRows());
        TreeSet colSet = new TreeSet(data.getColumns());

        Comparable[] _rows = (Comparable[]) rowSet.toArray(new Comparable[rowSet.size()]);
        Comparable[] _cols = (Comparable[]) colSet.toArray(new Comparable[colSet.size()]);

        // insert rows and columns in the right order
        for (Comparable r : _rows) {
            dataset.setValue(null, r, _cols[0]);
        }
        for (Comparable c : _cols) {
            dataset.setValue(null, _rows[0], c);
        }

        for (int i = 0; i < data.getValues().size(); i++) {
            dataset.addValue((Number) data.getValues().get(i), (Comparable) data.getRows().get(i), (Comparable) data.getColumns().get(i));
        }
    }

    @Override
    public String getImageMap(String id, int width, int height) {
        //Unfortunately we have to render it again, because the map is loaded lazily in another HTTP request
        render(width, height);
        return ChartUtilities.getImageMap(id, info);
    }

    @Extension
    public static class DescriptorImpl extends GraphSupportDescriptor {

        @Override
        public String getDisplayName() {
            return "JFreeChart";
        }
    }
}
