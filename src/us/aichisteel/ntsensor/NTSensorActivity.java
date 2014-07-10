/*
 * Copyright (C) 2014 Aichi Micro Intelligent Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * Distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.aichisteel.ntsensor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Paint.Align;
import android.text.Html;
import android.text.method.LinkMovementMethod;
//import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.ScatterChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import us.aichisteel.amisensor.*;
import us.aichisteel.misc.AmiFft;
import us.aichisteel.ntsensor.R;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.abs;

public class NTSensorActivity extends Activity implements AMISensorInterface {
	// private static final String TAG = NTSensorActivity.class.getSimpleName();
	private NTSensor mSensor;
	private Button btStart;
	private Button btStop;
	private Button btOffset;
	private RadioGroup rgSensorSelection;
	private RadioButton rbSensorSelectionNT;
	private RadioButton rbSensorSelectionUT;
	// private TextView tvText;

	private XYMultipleSeriesRenderer mRendererTime;
	private XYMultipleSeriesDataset mDatasetTime;
	private GraphicalView mGraphicalViewTime;

	private XYMultipleSeriesRenderer mRendererFreq;
	private XYMultipleSeriesDataset mDatasetFreq;
	private GraphicalView mGraphicalViewFreq;
	private final static double DEFAULT_TIME = 5.0;
	private AmiFft mFft;
	private boolean mIsChartTimeActive = true;
	private boolean mIsChartFreqActive = false;

	private double mMaxLevel = 0;
	private double mMaxLevelFreq = 0;

	@SuppressWarnings("deprecation")
	private XYMultipleSeriesDataset buildDataset(
			XYMultipleSeriesRenderer renderer, String[] titles, int[] colors,
			PointStyle[] styles, boolean[] fill_below, int[] fill_colors,
			boolean[] disp_value) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		List<double[]> x = new ArrayList<double[]>();
		List<double[]> y = new ArrayList<double[]>();
		for (int i = 0; i < titles.length; i++) {
			x.add(new double[] { 0 });
			y.add(new double[] { 0 });

			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[i]);
			r.setPointStyle(styles[i]);
			r.setFillPoints(true);
			r.setFillBelowLine(fill_below[i]);
			r.setFillBelowLineColor(fill_colors[i]);
			r.setDisplayChartValues(disp_value[i]);
			renderer.addSeriesRenderer(r);

			XYSeries series = new XYSeries(titles[i], 0);
			double[] xV = x.get(i);
			double[] yV = y.get(i);
			int seriesLength = xV.length;
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV[k], yV[k]);
			}
			dataset.addSeries(series);
		}
		return dataset;
	}

	private XYMultipleSeriesRenderer buildRenderer() {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		renderer.setChartTitleTextSize(18);
		renderer.setAxisTitleTextSize(15);
		renderer.setLabelsTextSize(15);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 20, 50, 15, 15 });
		renderer.setChartTitle("Time Domain");
		renderer.setXTitle("Time[sec]");
		renderer.setYTitle("Amplitude[nT]");
		renderer.setXAxisMin(0);
		renderer.setXAxisMax(DEFAULT_TIME);
		renderer.setYAxisMin(-100);
		renderer.setYAxisMax(100);
		renderer.setAxesColor(getResources().getColor(R.color.text_color));
		renderer.setLabelsColor(getResources().getColor(R.color.text_color));
		renderer.setXLabelsColor(getResources().getColor(R.color.text_color));
		renderer.setYLabelsColor(0, getResources().getColor(R.color.text_color));
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(getResources()
				.getColor(R.color.back_ground));
		renderer.setMarginsColor(getResources().getColor(R.color.margin));
		renderer.setGridColor(getResources().getColor(R.color.grid));
		renderer.setXLabels(12);
		renderer.setYLabels(12);
		renderer.setShowLegend(false);
		renderer.setShowGrid(true);
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setZoomButtonsVisible(false);
		renderer.setZoomEnabled(false, true);
		renderer.setPanEnabled(false, true);
		renderer.setPanLimits(new double[] { 0, DEFAULT_TIME, -1000, 1000 });
		renderer.setZoomLimits(new double[] { 0, DEFAULT_TIME, -1000, 1000 });
		return renderer;
	}

	private void setXLogGrid(XYMultipleSeriesRenderer renderer, double min,
			double max) {
		DecimalFormat form;
		double range = max - min;
		for (int i = 0; i <= 3; i++) {
			if (pow(10, i - 1) < 1) {
				form = new DecimalFormat("0.0");
			} else {
				form = new DecimalFormat("0");
			}
			for (int j = 1; j <= 10; j++) {
				if (range > 15) {
					renderer.addXTextLabel(10 * log10(j) + 10 * (i), "");
				} else {
					renderer.addXTextLabel(10 * log10(j) + 10 * (i),
							form.format(pow(10, i - 1) * j));
				}
			}
			renderer.addXTextLabel(i * 10, form.format(pow(10, i - 1)));
		}
	}

	private void setYLogGrid(XYMultipleSeriesRenderer renderer, double min,
			double max) {
		DecimalFormat form;
		double range = max - min;
		for (int i = 0; i <= 9; i++) {
			String zero = "";
			if (pow(10, i - 5) < 1) {
				for (int z = 0; z < abs(i - 5); z++)
					zero += "0";
				form = new DecimalFormat("0." + zero);
			} else
				form = new DecimalFormat("0");
			for (int j = 2; j <= 9; j++) {
				if (range > 15) {
					renderer.addYTextLabel(10 * log10(j) + 10 * (i), "");
				} else {
					renderer.addYTextLabel(10 * log10(j) + 10 * (i),
							form.format(pow(10, i - 5) * j));
				}
			}
			renderer.addYTextLabel((i) * 10, form.format(pow(10, i - 5)));
		}
	}

	private XYMultipleSeriesRenderer buildRendererFft() {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		renderer.setChartTitleTextSize(18);
		renderer.setAxisTitleTextSize(15);
		renderer.setLabelsTextSize(15);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 20, 50, 15, 15 });
		renderer.setChartTitle("Frequency Domain");
		renderer.setXTitle("Frequency[Hz]");
		renderer.setYTitle("Amplitude[nT]");
		renderer.setXAxisMin(0);
		renderer.setXAxisMax(30);
		renderer.setYAxisMin(10);
		renderer.setYAxisMax(90);
		renderer.setAxesColor(getResources().getColor(R.color.text_color));
		renderer.setLabelsColor(getResources().getColor(R.color.text_color));
		renderer.setXLabelsColor(getResources().getColor(R.color.text_color));
		renderer.setYLabelsColor(0, getResources().getColor(R.color.text_color));
		renderer.setApplyBackgroundColor(true);
		renderer.setBackgroundColor(getResources()
				.getColor(R.color.back_ground));
		renderer.setMarginsColor(getResources().getColor(R.color.margin));
		renderer.setGridColor(getResources().getColor(R.color.grid));
		renderer.setXLabels(12);
		renderer.setYLabels(12);
		renderer.setShowLegend(false);
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setZoomButtonsVisible(false);
		renderer.setZoomEnabled(true, true);
		renderer.setPanEnabled(true, true);
		renderer.setPanLimits(new double[] { 0, 30, 10, 90 });
		renderer.setZoomLimits(new double[] { 0, 30, 10, 90 });

		renderer.setXLabels(0);
		renderer.setYLabels(0);
		renderer.setShowCustomTextGrid(true);

		setXLogGrid(renderer, 0, 30);
		setYLogGrid(renderer, 20, 70);
		return renderer;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_ntsensor);

		mRendererTime = buildRenderer();
		mDatasetTime = buildDataset(mRendererTime, new String[] { "data" },
				new int[] { Color.BLUE },
				new PointStyle[] { PointStyle.POINT }, new boolean[] { false },
				new int[] { 0 }, new boolean[] { false });
		mGraphicalViewTime = ChartFactory.getLineChartView(
				getApplicationContext(), mDatasetTime, mRendererTime);
		LinearLayout layout = (LinearLayout) findViewById(R.id.plot_area);
		layout.addView(mGraphicalViewTime, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.MATCH_PARENT));

		mRendererFreq = buildRendererFft();
		mDatasetFreq = buildDataset(mRendererFreq,
				new String[] { "data", "max" }, new int[] { Color.BLUE,
						Color.RED }, new PointStyle[] { PointStyle.POINT,
						PointStyle.DIAMOND }, new boolean[] { true, false },
				new int[] { Color.LTGRAY, Color.RED }, new boolean[] { false,
						false });
		mGraphicalViewFreq = (GraphicalView) ChartFactory
				.getCombinedXYChartView(getBaseContext(), mDatasetFreq,
						mRendererFreq, new String[] { LineChart.TYPE,
								ScatterChart.TYPE });
		LinearLayout layout_fft = (LinearLayout) findViewById(R.id.fft_area);
		layout_fft.addView(mGraphicalViewFreq, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.MATCH_PARENT));

		setChart();

		btStart = (Button) findViewById(R.id.btStart);
		btStop = (Button) findViewById(R.id.btStop);
		btOffset = (Button) findViewById(R.id.btOffset);
		rgSensorSelection = (RadioGroup) findViewById(R.id.radioGroup1);
		rgSensorSelection.check(R.id.rd_nt);
		rbSensorSelectionNT = (RadioButton) findViewById(R.id.rd_nt);
		rbSensorSelectionUT = (RadioButton) findViewById(R.id.rd_ut);
		// tvText = (TextView)findViewById(R.id.tvSerial);
		btOffset.setVisibility(View.GONE);
		rgSensorSelection.setVisibility(View.GONE);

		mSensor = new NTSensor(this, this);
		mSensor.initializeSensor();
		enableButtons(mSensor.isReady());
		mSensor.setMaxTime(DEFAULT_TIME);
		mFft = new AmiFft((int) (DEFAULT_TIME * NTSensor.NTSENSOR_SPS),
				(1.0 / NTSensor.NTSENSOR_SPS));

		btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.startSensor();
				// Log.e(TAG, "NTSensorActivity: Start");
			}
		});

		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.stopSensor();
				// Log.e(TAG, "NTSensorActivity: Stop");
			}
		});

		btOffset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setOffset();
			}
		});

		rgSensorSelection
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						setSensor(checkedId);
					}
				});
		mGraphicalViewFreq.addZoomListener(new ZoomListener() {
			public void zoomApplied(ZoomEvent e) {
				setXLogGrid(mRendererFreq, mRendererFreq.getXAxisMin(),
						mRendererFreq.getXAxisMax());
				setYLogGrid(mRendererFreq, mRendererFreq.getYAxisMin(),
						mRendererFreq.getYAxisMax());
			}

			public void zoomReset() {
				setXLogGrid(mRendererFreq, 0, 30);
				setYLogGrid(mRendererFreq, 20, 70);
			}
		}, true, true);
		mGraphicalViewFreq.addPanListener(new PanListener() {
			public void panApplied() {
				setXLogGrid(mRendererFreq, mRendererFreq.getXAxisMin(),
						mRendererFreq.getXAxisMax());
				setYLogGrid(mRendererFreq, mRendererFreq.getYAxisMin(),
						mRendererFreq.getYAxisMax());
			}
		});

	}

	@Override
	public void onDestroy() {
		mSensor.finalizeSensor();
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setChart();
	}

	@Override
	public void onNewIntent(Intent intent) {
		mSensor.initializeSensor();
		enableButtons(mSensor.isReady());
	}

	@Override
	public void attachedSensor() {
		Toast.makeText(this, "Sensor attached.", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void detachedSensor() {
		Toast.makeText(this, "Sensor detached.", Toast.LENGTH_SHORT).show();
		enableButtons(mSensor.isReady());
		finish();
	}

	@Override
	public void dataReady() {
		DecimalFormat form = new DecimalFormat("0.00");
		double time = 0;
		double peakP = 0;
		double peakN = 0;
		mDatasetTime.getSeriesAt(0).clear();
		for (Double data : mSensor.getData()) {
			mDatasetTime.getSeriesAt(0).add(time, data);
			time += (1.0 / NTSensor.NTSENSOR_SPS);
			if (peakP < data) {
				peakP = data;
			}
			if (peakN > data) {
				peakN = data;
			}
		}
		mRendererTime.setChartTitle("Time Domain: "
				+ String.valueOf(form.format(peakP - peakN)) + "[nTpp]");
		mGraphicalViewTime.repaint();

		mMaxLevel = 0;
		mMaxLevelFreq = 0;
		mDatasetFreq.getSeriesAt(0).clear();
		mDatasetFreq.getSeriesAt(1).clear();
		if (mFft.AmiFftCalc(mSensor.getData())) {
			return;
		}

		for (int i = 0; i < mFft.getNum(); i++) {
			mDatasetFreq.getSeriesAt(0).add(
					10 * log10(mFft.getFreq().get(i)) + 10,
					10 * log10(mFft.getLevel().get(i)) + 50);
			// mDatasetFreq.getSeriesAt(0).add(mFft.getFreq().get(i),
			// mFft.getLevel().get(i) + 20);
			if (mFft.getFreq().get(i) >= pow(10,
					(mRendererFreq.getXAxisMin() - 10) / 10)
					&& mFft.getFreq().get(i) <= pow(10,
							(mRendererFreq.getXAxisMax() - 10) / 10)) {
				if (mMaxLevel < mFft.getLevel().get(i)) {
					mMaxLevel = mFft.getLevel().get(i);
					mMaxLevelFreq = mFft.getFreq().get(i);
				}
			}
		}

		// DecimalFormat form = new DecimalFormat("0.00");
		// tvText.setText("Peak=" + String.valueOf(form.format(mMaxLevel)) +
		// "[nT] at " + String.valueOf(form.format(mMaxLevelFreq)));
		mRendererFreq.setChartTitle("Frequency Domain: Peak="
				+ String.valueOf(form.format(mMaxLevel)) + "[nT] @"
				+ String.valueOf(form.format(mMaxLevelFreq)) + "[Hz]");
		mDatasetFreq.getSeriesAt(1).add(10 * log10(mMaxLevelFreq) + 10,
				10 * log10(mMaxLevel) + 50);
		mGraphicalViewFreq.repaint();
	}

	private void enableButtons(boolean enable) {
		btStart.setEnabled(enable);
		btStop.setEnabled(enable);
		btOffset.setEnabled(enable);
		rbSensorSelectionNT.setEnabled(enable);
		rbSensorSelectionUT.setEnabled(enable);

		int color;
		if (enable)
			color = getResources().getColor(R.color.text_color);
		else
			color = Color.GRAY;

		btStart.setTextColor(color);
		btStop.setTextColor(color);
		btOffset.setTextColor(color);
		rbSensorSelectionNT.setTextColor(color);
		rbSensorSelectionUT.setTextColor(color);
	}

	private void setSensor(int id) {
		if (id == R.id.rd_nt) {
			mRendererTime.setYTitle("Amplitude[nT]");
			mRendererFreq.setYTitle("Amplitude[nT]");
		} else if (id == R.id.rd_ut) {
			mRendererTime.setYTitle("Amplitude[uT]");
			mRendererFreq.setYTitle("Amplitude[uT]");
		}
	}

	private void setOffset() {
		mSensor.setOffset();
		double offset = mSensor.getLatestVoltage();
		Toast.makeText(this, "Offset=" + String.valueOf(offset) + "[V]",
				Toast.LENGTH_SHORT).show();
	}

	private enum MenuId {
		CHART_MENU, CHART_TIME, CHART_FREQ, DISPLAY_TIME, DISPLAY_TIME_1SEC, DISPLAY_TIME_2SEC, DISPLAY_TIME_3SEC, DISPLAY_TIME_4SEC, DISPLAY_TIME_5SEC, DISPLAY_TIME_10SEC, SENSOR_TYPE, SENSOR_TYPE_NT, SENSOR_TYPE_UT, SET_OFFSET, ABOUT;
	}

	public static <E extends Enum<E>> E fromOrdinal(Class<E> enumClass,
			int ordinal) {
		E[] enumArray = enumClass.getEnumConstants();
		return enumArray[ordinal];
	}

	private void setChart() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		Point p = new Point();
		disp.getSize(p);
		LinearLayout.LayoutParams params;
		if (mIsChartTimeActive && mIsChartFreqActive) {
			params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					p.y / 2 - 100);
			findViewById(R.id.plot_area).setLayoutParams(params);
			params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT);
			findViewById(R.id.fft_area).setLayoutParams(params);
		} else {
			params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					mIsChartTimeActive ? LayoutParams.FILL_PARENT : 0);
			findViewById(R.id.plot_area).setLayoutParams(params);
			params = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					mIsChartFreqActive ? LayoutParams.FILL_PARENT : 0);
			findViewById(R.id.fft_area).setLayoutParams(params);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {

		SubMenu typeMenu = menu.addSubMenu(Menu.NONE,
				MenuId.CHART_MENU.ordinal(), Menu.NONE, "Chart Type");
		typeMenu.add(Menu.NONE, MenuId.CHART_TIME.ordinal(), Menu.NONE,
				"Time Domain");
		typeMenu.add(Menu.NONE, MenuId.CHART_FREQ.ordinal(), Menu.NONE,
				"Frequency Domain");
		typeMenu.setGroupCheckable(Menu.NONE, true, false);
		typeMenu.findItem(MenuId.CHART_TIME.ordinal()).setChecked(true);

		SubMenu planeMenu = menu.addSubMenu(Menu.NONE,
				MenuId.DISPLAY_TIME.ordinal(), Menu.NONE, "Display Time[sec]");
		planeMenu.add(Menu.NONE, MenuId.DISPLAY_TIME_1SEC.ordinal(), Menu.NONE,
				"1");
		planeMenu.add(Menu.NONE, MenuId.DISPLAY_TIME_2SEC.ordinal(), Menu.NONE,
				"2");
		planeMenu.add(Menu.NONE, MenuId.DISPLAY_TIME_3SEC.ordinal(), Menu.NONE,
				"3");
		planeMenu.add(Menu.NONE, MenuId.DISPLAY_TIME_4SEC.ordinal(), Menu.NONE,
				"4");
		planeMenu.add(Menu.NONE, MenuId.DISPLAY_TIME_5SEC.ordinal(), Menu.NONE,
				"5");
		planeMenu.add(Menu.NONE, MenuId.DISPLAY_TIME_10SEC.ordinal(),
				Menu.NONE, "10");
		planeMenu.setGroupCheckable(Menu.NONE, true, true);
		planeMenu.findItem(MenuId.DISPLAY_TIME_5SEC.ordinal()).setChecked(true);

		SubMenu sensorMenu = menu.addSubMenu(Menu.NONE,
				MenuId.SENSOR_TYPE.ordinal(), Menu.NONE, "Sensor Type");
		sensorMenu.add(Menu.NONE, MenuId.SENSOR_TYPE_NT.ordinal(), Menu.NONE,
				"nT Sensor");
		sensorMenu.add(Menu.NONE, MenuId.SENSOR_TYPE_UT.ordinal(), Menu.NONE,
				"uT Sensor");
		sensorMenu.setGroupCheckable(Menu.NONE, true, true);
		sensorMenu.findItem(MenuId.SENSOR_TYPE_NT.ordinal()).setChecked(true);

		menu.add(Menu.NONE, MenuId.SET_OFFSET.ordinal(), Menu.NONE,
				"Set Offset");
		menu.add(Menu.NONE, MenuId.ABOUT.ordinal(), Menu.NONE, "About");

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		MenuId menuid = fromOrdinal(MenuId.class, item.getItemId());
		switch (menuid) {
		case CHART_TIME:
			mIsChartTimeActive = item.isChecked() ? false : true;
			item.setChecked(mIsChartTimeActive);
			setChart();
			break;
		case CHART_FREQ:
			mIsChartFreqActive = item.isChecked() ? false : true;
			item.setChecked(mIsChartFreqActive);
			setChart();
			break;
		case DISPLAY_TIME:
			break;
		case DISPLAY_TIME_1SEC:
		case DISPLAY_TIME_2SEC:
		case DISPLAY_TIME_3SEC:
		case DISPLAY_TIME_4SEC:
		case DISPLAY_TIME_5SEC:
		case DISPLAY_TIME_10SEC:
			int time = Integer.parseInt(item.getTitle().toString());
			mSensor.setMaxTime(time);
			mRendererTime.setXAxisMax(time);
			mFft = new AmiFft((int) (time * NTSensor.NTSENSOR_SPS),
					(1.0 / NTSensor.NTSENSOR_SPS));
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SENSOR_TYPE:
			break;
		case SENSOR_TYPE_NT:
			setSensor(R.id.rd_nt);
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SENSOR_TYPE_UT:
			setSensor(R.id.rd_ut);
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SET_OFFSET:
			setOffset();
			break;
		case ABOUT:
			((TextView) new AlertDialog.Builder(NTSensorActivity.this)
					.setTitle("About")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setMessage(
							Html.fromHtml("<p>AMI Nano/Micro Tesla Sensor Application<br>"
									+ "<a href=\"http://www.aichi-mi.com\">Aichi Micro Intelligent Corporation</a></p>"
									+ "<p>This software includes the following works that are distributed in the Apache License 2.0.<br>"
									+ " - Physicaloid Library<br>"
									+ " - Achartengine 1.1.0</p>")).show()
					.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());
			break;

		}
		mGraphicalViewTime.repaint();
		mGraphicalViewFreq.repaint();
		return false;
	}
}
