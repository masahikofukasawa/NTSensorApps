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

package us.aichisteel.ntsensor3axis;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Paint.Align;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
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
import us.aichisteel.ntsensor3axis.R;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.abs;

public class NTSensor3AxisActivity extends Activity implements AMISensorInterface {
	private static final String TAG = NTSensor3AxisActivity.class.getSimpleName();
	private NTSensor3Axis mSensor;
	private Button btStart;
	private Button btStop;
	private Button btOffset;
	private RadioGroup rgSensorSelection;
	private RadioButton rbSensorSelectionNT;
	private RadioButton rbSensorSelectionDNT;
	private RadioButton rbSensorSelectionUT;
	// private TextView tvText;

	private XYMultipleSeriesRenderer mRendererTime;
	private XYMultipleSeriesDataset mDatasetTime;
	private GraphicalView mGraphicalViewTime;

	private XYMultipleSeriesRenderer mRendererFreq;
	private XYMultipleSeriesDataset mDatasetFreq;
	private GraphicalView mGraphicalViewFreq;
	private final static double DEFAULT_TIME = 2.0;
	private AmiFft[] mFft = new AmiFft[3];
	private AmiFft mFftPower;
	private boolean mIsChartTimeActive = true;
	private boolean mIsChartFreqActive = false;

	private double mMaxLevel = 0;
	private double mMaxLevelFreq = 0;

	private int mAlertThreshold = 70;

	private String mUnit = "nT";

	private static final byte AXIS_SELECTION_ALL = 0x07;
	private static final byte AXIS_SELECTION_X = 0x01;
	private static final byte AXIS_SELECTION_Y = 0x02;
	private static final byte AXIS_SELECTION_Z = 0x04;
	private static final byte AXIS_SELECTION_POWER = 0x08;
	private byte mAxisSelection = AXIS_SELECTION_ALL;
	
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
		renderer.setPanLimits(new double[] { 0, DEFAULT_TIME, -4000, 4000 });
		renderer.setZoomLimits(new double[] { 0, DEFAULT_TIME, -4000, 4000 });
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
		mDatasetTime = buildDataset(mRendererTime,
				new String[] { "x", "y", "z", "power" },
				new int[] { Color.RED, Color.GREEN, Color.BLUE, Color.BLACK }, 
				new PointStyle[] { PointStyle.POINT, PointStyle.POINT, PointStyle.POINT, PointStyle.POINT }, 
				new boolean[] { false, false, false, false }, 
				new int[] { 0, 0, 0, 0 }, 
				new boolean[] { false, false, false, false });
		mGraphicalViewTime = ChartFactory.getLineChartView(
				getApplicationContext(), mDatasetTime, mRendererTime);
		LinearLayout layout = (LinearLayout) findViewById(R.id.plot_area);
		layout.addView(mGraphicalViewTime, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.MATCH_PARENT));

		mRendererFreq = buildRendererFft();
		mDatasetFreq = buildDataset(mRendererFreq,
				new String[] { "x", "y", "z", "power" }, 
				new int[] { Color.RED, Color.GREEN, Color.BLUE, Color.BLACK }, 
				new PointStyle[] { PointStyle.POINT, PointStyle.POINT, PointStyle.POINT, PointStyle.POINT },
				new boolean[] { false, false, false, false },
				new int[] { 0, 0, 0, 0 }, 
				new boolean[] { false, false, false, false });
		mGraphicalViewFreq = ChartFactory.getLineChartView(
				getApplicationContext(), mDatasetFreq, mRendererFreq);
/*
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
*/
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
		rbSensorSelectionDNT = (RadioButton) findViewById(R.id.rd_dnt);
		rbSensorSelectionUT = (RadioButton) findViewById(R.id.rd_ut);
		// tvText = (TextView)findViewById(R.id.tvSerial);
		btOffset.setVisibility(View.GONE);
		rgSensorSelection.setVisibility(View.GONE);

		mSensor = new NTSensor3Axis(this, this);
		mSensor.initializeSensor();
		enableButtons(mSensor.isReady());
		mSensor.setMaxTime(DEFAULT_TIME);
//		setAlertThresholdLine();

		for(int i=0; i<3; i++)
		{
			mFft[i] = new AmiFft((int) (DEFAULT_TIME * NTSensor3Axis.NTSENSOR_SPS),
					(1.0 / NTSensor3Axis.NTSENSOR_SPS));
		}
		mFftPower = new AmiFft((int) (DEFAULT_TIME * NTSensor3Axis.NTSENSOR_SPS),
				(1.0 / NTSensor3Axis.NTSENSOR_SPS));
		
		btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.startSensor();
				Log.e(TAG, "Start");
			}
		});

		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.stopSensor();
				Log.e(TAG, "Stop");
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
		Log.e(TAG, "detachedSensor Called");
		Toast.makeText(this, "Sensor detached.", Toast.LENGTH_SHORT).show();
		enableButtons(mSensor.isReady());
		finish();
	}

	@Override
	public void dataReady() {
//		DecimalFormat form = new DecimalFormat("0.00");
		double time = 0;
//		double peakP = 0;
//		double peakN = 0;
		
		for(int axis=0; axis<4; axis++)
			mDatasetTime.getSeriesAt(axis).clear();

		for(int axis=0; axis<3; axis++)
		{
			time = 0;
			if( (mAxisSelection & (byte)(1<<axis)) != 0 )
			{
				for (Double[] data : mSensor.getData()) {
					mDatasetTime.getSeriesAt(axis).add(time, data[axis]);
					time += (1.0 / NTSensor3Axis.NTSENSOR_SPS);
		/*
					if (peakP < data[0]) {
						peakP = data[0];
					}
					if (peakN > data[0]) {
						peakN = data[0];
					}
		*/
				}
			}
			
/*
		if (mIsChartTimeActive) {
			if (peakP > mAlertThreshold | peakN < -1 * mAlertThreshold) {
				mRendererTime.setBackgroundColor(getResources().getColor(
						R.color.caution_color));
				beep();
			} else {
				mRendererTime.setBackgroundColor(getResources().getColor(
						R.color.back_ground));
			}
		}

		mRendererTime.setChartTitle("Time Domain: "
				+ String.valueOf(form.format(peakP - peakN)) + "[" + mUnit
				+ "pp]");
*/
			
//		mMaxLevel = 0;
//		mMaxLevelFreq = 0;
//		mDatasetFreq.getSeriesAt(1).clear();
		}
		
		time = 0;
		if( (mAxisSelection & AXIS_SELECTION_POWER) != 0 )
		{
			for (Double data : mSensor.getPowerData()) {
				mDatasetTime.getSeriesAt(3).add(time, data);
				time += (1.0 / NTSensor3Axis.NTSENSOR_SPS);
			}
		}
		
		if(mIsChartTimeActive) mGraphicalViewTime.repaint();
		
		if(mIsChartFreqActive)
		{
			for(int axis=0; axis<4; axis++)
				mDatasetFreq.getSeriesAt(axis).clear();
			
			for(int axis=0; axis<3; axis++)
			{
				if( (mAxisSelection & (byte)(1<<axis)) != 0 )
				{
					if (mFft[axis].AmiFftCalc(mSensor.getData(axis))) {
						return;
					}
			
					for (int i = 0; i < mFft[axis].getNum(); i++) {
						mDatasetFreq.getSeriesAt(axis).add(
								10 * log10(mFft[axis].getFreq().get(i)) + 10,
								10 * log10(mFft[axis].getLevel().get(i)) + 50);
						// mDatasetFreq.getSeriesAt(0).add(mFft.getFreq().get(i),
						// mFft.getLevel().get(i) + 20);
		
		//				if (mFft[axis].getFreq().get(i) >= pow(10,
		//						(mRendererFreq.getXAxisMin() - 10) / 10)
		//						&& mFft[axis].getFreq().get(i) <= pow(10,
		//								(mRendererFreq.getXAxisMax() - 10) / 10)) {
		//					if (mMaxLevel < mFft[axis].getLevel().get(i)) {
		//						mMaxLevel = mFft[axis].getLevel().get(i);
		//						mMaxLevelFreq = mFft[axis].getFreq().get(i);
		//					}
		//				}
					}
				}
			}
			if( (mAxisSelection & AXIS_SELECTION_POWER) != 0 )
			{
				if (mFftPower.AmiFftCalc(mSensor.getPowerData())) {
					return;
				}
		
				for (int i = 0; i < mFftPower.getNum(); i++) {
					mDatasetFreq.getSeriesAt(3).add(
							10 * log10(mFftPower.getFreq().get(i)) + 10,
							10 * log10(mFftPower.getLevel().get(i)) + 50);
				}
			}
			mGraphicalViewFreq.repaint();
		}
		
		// DecimalFormat form = new DecimalFormat("0.00");
		// tvText.setText("Peak=" + String.valueOf(form.format(mMaxLevel)) +
		// "[nT] at " + String.valueOf(form.format(mMaxLevelFreq)));
/*
		mRendererFreq.setChartTitle("Frequency Domain: Peak="
				+ String.valueOf(form.format(mMaxLevel)) + "[" + mUnit + "] @"
				+ String.valueOf(form.format(mMaxLevelFreq)) + "[Hz]");
		mDatasetFreq.getSeriesAt(1).add(10 * log10(mMaxLevelFreq) + 10,
				10 * log10(mMaxLevel) + 50);
*/
	}

	private void enableButtons(boolean enable) {
		btStart.setEnabled(enable);
		btStop.setEnabled(enable);
		btOffset.setEnabled(enable);
		rbSensorSelectionNT.setEnabled(enable);
		rbSensorSelectionDNT.setEnabled(enable);
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
		rbSensorSelectionDNT.setTextColor(color);
		rbSensorSelectionUT.setTextColor(color);
	}

	private void setSensor(int id) {
		if (id == R.id.rd_nt) {
			mUnit = "nT";
			mSensor.setSensitivity(4,4,4);
		} else if (id == R.id.rd_dnt) {
			mUnit = "nT";
			mSensor.setSensitivity(2,2,2);
		} else if (id == R.id.rd_ut) {
			mUnit = "uT";
			mSensor.setSensitivity(4,4,4);
		}
		mRendererTime.setYTitle("Amplitude[" + mUnit + "]");
		mRendererFreq.setYTitle("Amplitude[" + mUnit + "]");
	}

	private void setOffset() {
		mSensor.setOffset();
		double[] offset = mSensor.getLatestVoltage();
		Toast.makeText(this, "Offset=" + String.valueOf(offset[0]) + ","+ String.valueOf(offset[1]) + ","+ String.valueOf(offset[2]) ,
				Toast.LENGTH_SHORT).show();
	}

	private enum MenuId {
		CHART_MENU, CHART_TIME, CHART_FREQ, DISPLAY_TIME, DISPLAY_TIME_1SEC, DISPLAY_TIME_2SEC, DISPLAY_TIME_3SEC, DISPLAY_TIME_4SEC, DISPLAY_TIME_5SEC, DISPLAY_TIME_10SEC, SENSOR_TYPE, SENSOR_TYPE_NT, SENSOR_TYPE_DNT, SENSOR_TYPE_UT, SET_OFFSET, ABOUT, CAUTION_SETTINGS, AXIS_SELECTION, AXIS_ALL, AXIS_X, AXIS_Y, AXIS_Z, AXIS_POWER;
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
		planeMenu.findItem(MenuId.DISPLAY_TIME_2SEC.ordinal()).setChecked(true);

		SubMenu sensorMenu = menu.addSubMenu(Menu.NONE,
				MenuId.SENSOR_TYPE.ordinal(), Menu.NONE, "Sensor Type");
		sensorMenu.add(Menu.NONE, MenuId.SENSOR_TYPE_NT.ordinal(), Menu.NONE,
				"nT Sensor");
		sensorMenu.add(Menu.NONE, MenuId.SENSOR_TYPE_DNT.ordinal(), Menu.NONE,
				"Differential nT Sensor");
		sensorMenu.add(Menu.NONE, MenuId.SENSOR_TYPE_UT.ordinal(), Menu.NONE,
				"uT Sensor");
		sensorMenu.setGroupCheckable(Menu.NONE, true, true);
		sensorMenu.findItem(MenuId.SENSOR_TYPE_NT.ordinal()).setChecked(true);

		menu.add(Menu.NONE, MenuId.SET_OFFSET.ordinal(), Menu.NONE,
				"Set Offset");
		menu.add(Menu.NONE, MenuId.CAUTION_SETTINGS.ordinal(), Menu.NONE,
				"Alert Threshold");

		SubMenu axisMenu = menu.addSubMenu(Menu.NONE,
				MenuId.AXIS_SELECTION.ordinal(), Menu.NONE, "Axis Selection");
		axisMenu.add(Menu.NONE, MenuId.AXIS_ALL.ordinal(), Menu.NONE,
				"All 3Axis");
		axisMenu.add(Menu.NONE, MenuId.AXIS_X.ordinal(), Menu.NONE,
				"X");
		axisMenu.add(Menu.NONE, MenuId.AXIS_Y.ordinal(), Menu.NONE,
				"Y");
		axisMenu.add(Menu.NONE, MenuId.AXIS_Z.ordinal(), Menu.NONE,
				"Z");
		axisMenu.add(Menu.NONE, MenuId.AXIS_POWER.ordinal(), Menu.NONE,
				"Power");
		axisMenu.setGroupCheckable(Menu.NONE, true, true);
		axisMenu.findItem(MenuId.AXIS_ALL.ordinal()).setChecked(true);

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
			for(int i=0; i<3; i++)
			{
				mFft[i] = new AmiFft((int) (time * NTSensor3Axis.NTSENSOR_SPS),
						(1.0 / NTSensor3Axis.NTSENSOR_SPS));				
			}
			mFftPower = new AmiFft((int) (time * NTSensor3Axis.NTSENSOR_SPS),
					(1.0 / NTSensor3Axis.NTSENSOR_SPS));
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SENSOR_TYPE:
			break;
		case SENSOR_TYPE_NT:
			setSensor(R.id.rd_nt);
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SENSOR_TYPE_DNT:
			setSensor(R.id.rd_dnt);
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SENSOR_TYPE_UT:
			setSensor(R.id.rd_ut);
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SET_OFFSET:
			setOffset();
			break;
		case CAUTION_SETTINGS:
			displaySeakBar();
			break;
		case AXIS_ALL:
			mAxisSelection = AXIS_SELECTION_ALL;
			item.setChecked(item.isChecked() ? false : true);
			break;
		case AXIS_X:
			mAxisSelection = AXIS_SELECTION_X;
			item.setChecked(item.isChecked() ? false : true);
			break;
		case AXIS_Y:
			mAxisSelection = AXIS_SELECTION_Y;
			item.setChecked(item.isChecked() ? false : true);
			break;
		case AXIS_Z:
			mAxisSelection = AXIS_SELECTION_Z;
			item.setChecked(item.isChecked() ? false : true);
			break;
		case AXIS_POWER:
			mAxisSelection = AXIS_SELECTION_POWER;
			item.setChecked(item.isChecked() ? false : true);
			break;
		case ABOUT:
			((TextView) new AlertDialog.Builder(NTSensor3AxisActivity.this)
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
		default:
			break;
		}
		mGraphicalViewTime.repaint();
		mGraphicalViewFreq.repaint();
		return false;
	}

	public void beep() {
		ToneGenerator toneGenerator = new ToneGenerator(
				AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
		toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
	}
/*
	public void setAlertThresholdLine() {
		mDatasetTime.getSeriesAt(1).clear();
		mDatasetTime.getSeriesAt(2).clear();
		for (int time = 0; time <= mSensor.getMaxTime(); time++) {
			mDatasetTime.getSeriesAt(1).add(time, -1 * mAlertThreshold);
			mDatasetTime.getSeriesAt(2).add(time, 1 * mAlertThreshold);
		}
		mGraphicalViewTime.repaint();
	}
*/
	public void displaySeakBar() {
		final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.seekbar_dialog,
				(ViewGroup) findViewById(R.id.seek_dialog));
		final TextView item1 = (TextView) layout.findViewById(R.id.textView1);

		popDialog.setIcon(android.R.drawable.btn_star_big_on);
		popDialog.setTitle("Please set alert threshold.");
		popDialog.setView(layout);
		SeekBar seek1 = (SeekBar) layout.findViewById(R.id.seekBar1);
		item1.setText("Threshold : +/-" + mAlertThreshold + " [" + mUnit + "]");
		seek1.setProgress(mAlertThreshold);
		seek1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// Do something here with new value
				item1.setText("Threshold : +/-" + progress + " [" + mUnit + "]");
				mAlertThreshold = progress;
//				setAlertThresholdLine();
			}

			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub

			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}
		});
		popDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(
								NTSensor3AxisActivity.this,
								"Alert Threshold=" + mAlertThreshold + "["
										+ mUnit + "]", Toast.LENGTH_SHORT)
								.show();
						dialog.dismiss();
					}
				});
		popDialog.create();
		popDialog.show();
	}
}
