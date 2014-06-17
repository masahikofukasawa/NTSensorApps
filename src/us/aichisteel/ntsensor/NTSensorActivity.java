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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import us.aichisteel.amisensor.*;
import us.aichisteel.ntsensor.R;

public class NTSensorActivity extends Activity implements
AMISensorInterface {

	private NTSensor mSensor;
	private Button btStart;
	private Button btStop;
	private Button btOffset;
	private RadioGroup rgSensorSelection;
	private RadioButton rbSensorSelectionNT;
	private RadioButton rbSensorSelectionUT;


	private XYMultipleSeriesRenderer mRenderer;
	private XYMultipleSeriesDataset dataset;
	private GraphicalView graphicalView;

	private double mXaxisMax = 5.0;


	private XYMultipleSeriesDataset buildDataset(String[] titles,
			List<double[]> xValues, List<double[]> yValues) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		addXYSeries(dataset, titles, xValues, yValues, 0);
		return dataset;
	}

	private void addXYSeries(XYMultipleSeriesDataset dataset, String[] titles,
			List<double[]> xValues, List<double[]> yValues, int scale) {
		int length = titles.length;
		for (int i = 0; i < length; i++) {
			XYSeries series = new XYSeries(titles[i], scale);
			double[] xV = xValues.get(i);
			double[] yV = yValues.get(i);
			int seriesLength = xV.length;
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV[k], yV[k]);
			}
			dataset.addSeries(series);
		}
	}

	private XYMultipleSeriesRenderer buildRenderer(int[] colors,
			PointStyle[] styles) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		renderer.setAxisTitleTextSize(20);
		renderer.setLabelsTextSize(20);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 20, 60, 15, 10 });
		int length = colors.length;
		for (int i = 0; i < length; i++) {
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[i]);
			r.setPointStyle(styles[i]);
			renderer.addSeriesRenderer(r);
		}
		return renderer;
	}

	private void setChartSettings(XYMultipleSeriesRenderer renderer,
			String title, String xTitle, String yTitle, double xMin,
			double xMax, double yMin, double yMax, int axesColor,
			int labelsColor) {
		renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setXAxisMin(xMin);
		renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
		renderer.setXLabelsColor(labelsColor);
		renderer.setYLabelsColor(0, labelsColor);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ntsensor);

		String[] titles = new String[] { "data" };
		List<double[]> x = new ArrayList<double[]>();
		for (int i = 0; i < titles.length; i++) {
			x.add(new double[] { 0 });
		}
		List<double[]> values = new ArrayList<double[]>();
		values.add(new double[] { 0 });
		int[] colors = new int[] { Color.RED };
		PointStyle[] styles = new PointStyle[] { PointStyle.POINT };

		mRenderer = buildRenderer(colors, styles);
		int length = mRenderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) mRenderer.getSeriesRendererAt(i))
					.setFillPoints(true);
		}
		setChartSettings(mRenderer, "", "Time[sec]", "Level[nT]", 0, mXaxisMax,
				-100, 100, getResources().getColor(R.color.dark_blue),
				getResources().getColor(R.color.dark_blue));
		mRenderer.setApplyBackgroundColor(true);
		mRenderer.setBackgroundColor(getResources().getColor(
				R.color.back_ground));
		mRenderer.setMarginsColor(getResources().getColor(R.color.margin));
		mRenderer.setGridColor(getResources().getColor(R.color.grid));
		mRenderer.setXLabels(12);
		mRenderer.setYLabels(12);
		mRenderer.setShowLegend(false);
		mRenderer.setShowGrid(true);
		mRenderer.setXLabelsAlign(Align.CENTER);
		mRenderer.setYLabelsAlign(Align.RIGHT);
		mRenderer.setZoomButtonsVisible(false);
		mRenderer.setZoomEnabled(false, true);
		mRenderer.setPanEnabled(false, true);
		mRenderer.setPanLimits(new double[] { 0, 60, -1000, 1000 });
		mRenderer.setZoomLimits(new double[] { 0, 60, -1000, 1000 });
		dataset = buildDataset(titles, x, values);

		LinearLayout layout = (LinearLayout) findViewById(R.id.plot_area);
		graphicalView = ChartFactory.getLineChartView(getApplicationContext(),
				dataset, mRenderer);
		layout.addView(graphicalView, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		btStart = (Button) findViewById(R.id.btStart);
		btStop = (Button) findViewById(R.id.btStop);
		btOffset = (Button) findViewById(R.id.btOffset);
		rgSensorSelection = (RadioGroup) findViewById(R.id.radioGroup1);
		rgSensorSelection.check(R.id.rd_nt);
		rbSensorSelectionNT = (RadioButton) findViewById(R.id.rd_nt);
		rbSensorSelectionUT = (RadioButton) findViewById(R.id.rd_ut);

		btOffset.setVisibility(View.GONE);

		mSensor = new NTSensor(this);
		mSensor.initialize(this);
		enableButtons(mSensor.isReady());

		rgSensorSelection.setVisibility(View.GONE);

		btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.startSensor();
			}
		});

		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.stopSensor();
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
	}

	@Override
	public void onDestroy() {
		mSensor.finalize(this);
		super.onDestroy();
	}
	@Override
	public void onNewIntent(Intent intent) {
		mSensor.initialize(this);
		enableButtons(mSensor.isReady());
	}

	@Override
	public void attachedSensor() {
		enableButtons(mSensor.isReady());
	}

	@Override
	public void detachedSensor() {
		enableButtons(mSensor.isReady());
		finish();
	}

	@Override
	public void dataReady() {
		mSensor.setMaxTime(mRenderer.getXAxisMax());
		double time = 0;
		dataset.getSeriesAt(0).clear();
		for (Double data : mSensor.getData()) {
			dataset.getSeriesAt(0).add(time, data);
			time += (1.0 / NTSensor.NTSENSOR_SPS);
		}
		graphicalView.repaint();
	}

	private void enableButtons(boolean enable) {
		btStart.setEnabled(enable);
		btStop.setEnabled(enable);
		btOffset.setEnabled(enable);
		rbSensorSelectionNT.setEnabled(enable);
		rbSensorSelectionUT.setEnabled(enable);

		int color;
		if (enable)
			color = getResources().getColor(R.color.dark_blue);
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
			mRenderer.setYTitle("Level[nT]");
		} else if (id == R.id.rd_ut) {
			mRenderer.setYTitle("Level[uT]");
		}
	}

	private void setOffset() {
		double offset = mSensor.setOffset();
		Toast.makeText(this, "Offset=" + String.valueOf(offset) + "[V]",
				Toast.LENGTH_SHORT).show();
	}

	private enum MenuId {
		DISPLAY_TIME, DISPLAY_TIME_1SEC, DISPLAY_TIME_2SEC, DISPLAY_TIME_3SEC, DISPLAY_TIME_4SEC, DISPLAY_TIME_5SEC, DISPLAY_TIME_10SEC, SENSOR_TYPE, SENSOR_TYPE_NT, SENSOR_TYPE_UT, SET_OFFSET, ABOUT;
	}

	public static <E extends Enum<E>> E fromOrdinal(Class<E> enumClass,
			int ordinal) {
		E[] enumArray = enumClass.getEnumConstants();
		return enumArray[ordinal];
	}

	public boolean onCreateOptionsMenu(Menu menu) {
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
		planeMenu.findItem(5).setChecked(true);

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
			mRenderer.setXAxisMax(time);
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SENSOR_TYPE:
			break;
		case SENSOR_TYPE_NT:
			mRenderer.setYTitle("Level[nT]");
			item.setChecked(item.isChecked() ? false : true);
			break;
		case SENSOR_TYPE_UT:
			mRenderer.setYTitle("Level[uT]");
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
											+ " - Achartengine 1.1.0</p>"
											))
					.show()
					.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());
			break;

		}
		if (graphicalView != null) {
			graphicalView.repaint();
		}
		return false;
	}
}
