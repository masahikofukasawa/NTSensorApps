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
import android.content.res.Configuration;
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

public class NTSensorActivity extends Activity implements AMISensorInterface {

	private NTSensor mSensor;
	private Button btStart;
	private Button btStop;
	private Button btOffset;
	private RadioGroup rgSensorSelection;
	private RadioButton rbSensorSelectionNT;
	private RadioButton rbSensorSelectionUT;
	private XYMultipleSeriesRenderer mRenderer;
	private XYMultipleSeriesDataset mDataset;
	private GraphicalView mGraphicalView;
	private double mXaxisMax = 5.0;

	private XYMultipleSeriesDataset buildDataset(
			XYMultipleSeriesRenderer renderer, String[] titles, int[] colors,
			PointStyle[] styles) {
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
		renderer.setAxisTitleTextSize(20);
		renderer.setLabelsTextSize(20);
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 20, 60, 15, 10 });
		renderer.setChartTitle("");
		renderer.setXTitle("Time[sec]");
		renderer.setYTitle("Level[nT]");
		renderer.setXAxisMin(0);
		renderer.setXAxisMax(mXaxisMax);
		renderer.setYAxisMin(-100);
		renderer.setYAxisMax(100);
		renderer.setAxesColor(getResources().getColor(R.color.dark_blue));
		renderer.setLabelsColor(getResources().getColor(R.color.dark_blue));
		renderer.setXLabelsColor(getResources().getColor(R.color.dark_blue));
		renderer.setYLabelsColor(0, getResources().getColor(R.color.dark_blue));
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
		renderer.setPanLimits(new double[] { 0, 60, -1000, 1000 });
		renderer.setZoomLimits(new double[] { 0, 60, -1000, 1000 });
		return renderer;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ntsensor);

		mRenderer = buildRenderer();
		mDataset = buildDataset(mRenderer, new String[] { "data" },
				new int[] { Color.RED }, new PointStyle[] { PointStyle.POINT });

		LinearLayout layout = (LinearLayout) findViewById(R.id.plot_area);
		mGraphicalView = ChartFactory.getLineChartView(getApplicationContext(),
				mDataset, mRenderer);
		layout.addView(mGraphicalView, new LayoutParams(
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
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
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
		mDataset.getSeriesAt(0).clear();
		for (Double data : mSensor.getData()) {
			mDataset.getSeriesAt(0).add(time, data);
			time += (1.0 / NTSensor.NTSENSOR_SPS);
		}
		mGraphicalView.repaint();
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
		mSensor.setOffset();
		double offset = mSensor.getLatestVoltage();
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
									+ " - Achartengine 1.1.0</p>")).show()
					.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());
			break;

		}
		if (mGraphicalView != null) {
			mGraphicalView.repaint();
		}
		return false;
	}
}
