package us.aichisteel.ntsensor;

import java.io.IOException;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.hardware.usb.UsbManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import us.aichisteel.ntsensor.R;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class NTSensorActivity extends Activity {

	private NTSensorData sensorData = new NTSensorData();

	Handler mHandler = new Handler();

	private static final String CR = "\r";
	private String stTransmit = CR;
	private String stStartCommand = "a";
	private String stStopCommand = "s";

	Physicaloid mSerial;

	private Button btStart;
	private Button btStop;
	private Button btOffset;
	private StringBuilder mText = new StringBuilder();
	private int iBaudRate = 115200;
	private RadioGroup rgSensorSelection;
	private RadioButton rbSensorSelectionNT;
	private RadioButton rbSensorSelectionUT;

	private boolean mRunningMainLoop;

	private static final String ACTION_USB_PERMISSION = "com.example.ntsensor.USB_PERMISSION";

	private XYMultipleSeriesRenderer mRenderer;
	private XYMultipleSeriesDataset dataset;
	private GraphicalView graphicalView;

	private double mXaxisMax = 5.0;

	// Linefeed
	private final static String BR = System.getProperty("line.separator");

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

		mRunningMainLoop = false;

		btStart = (Button) findViewById(R.id.btStart);
		btStop = (Button) findViewById(R.id.btStop);
		btOffset = (Button) findViewById(R.id.btOffset);
		rgSensorSelection = (RadioGroup) findViewById(R.id.radioGroup1);
		rgSensorSelection.check(R.id.rd_nt);
		rbSensorSelectionNT = (RadioButton) findViewById(R.id.rd_nt);
		rbSensorSelectionUT = (RadioButton) findViewById(R.id.rd_ut);

		btOffset.setVisibility(View.GONE);
		rgSensorSelection.setVisibility(View.GONE);

		// get service
		mSerial = new Physicaloid(this);

		// listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		openUsbSerial();

		btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
					startSensor();
			}
		});

		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopSensor();
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
		mSerial.close();
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
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
			color = Color.LTGRAY;

		btStart.setTextColor(color);
		btStop.setTextColor(color);
		btOffset.setTextColor(color);
		rbSensorSelectionNT.setTextColor(color);
		rbSensorSelectionUT.setTextColor(color);
	}

	private void openUsbSerial() {
		if (mSerial == null) {
			enableButtons(false);
			return;
		}

		if (!mSerial.isOpened()) {
			if (!mSerial.open()) {
				enableButtons(false);
				return;
			} else {
				mSerial.setConfig(new UartConfig(iBaudRate, 8, 1, 0, false,
						false));
				enableButtons(true);
			}
		}

		if (!mRunningMainLoop) {
			mainloop();
		}
	}

	private void mainloop() {
		mRunningMainLoop = true;
		new Thread(mLoop).start();
	}

	public int mDataCount = 0;
	public double mTime = 0.0;

	private Runnable mLoop = new Runnable() {
		@Override
		public void run() {
			int len;
			byte[] rbuf = new byte[4096];

			for (;;) {
				len = mSerial.read(rbuf);

				if (len > 0) {
					sensorData.addData(rbuf, len);
					sensorData.setMaxTime(mRenderer.getXAxisMax());
					setSerialDataToTextView(rbuf, len);

					mHandler.post(new Runnable() {
						@Override
						public void run() {
							try {
								double time = 0;
								dataset.getSeriesAt(0).clear();
								for (Double data : sensorData.getData()) {
									dataset.getSeriesAt(0).add(time, data);
									time += (1.0 / NTSensorData.NTSENSOR_SPS);
								}
								graphicalView.repaint();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (!mRunningMainLoop) {
					return;
				}
			}
		}
	};

	boolean lastDataIs0x0D = false;
	int count = 0;

	void setSerialDataToTextView(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			// "\r":CR(0x0D) "\n":LF(0x0A)
			if (rbuf[i] == 0x0D) {
				mText.append(BR);
			} else if (rbuf[i] == 0x0A) {
				// if (iTarget != MENU_TARGET_NTSENSOR) {
				mText.append(BR);
				// }
			} else if ((rbuf[i] == 0x0D) && (rbuf[i + 1] == 0x0A)) {
				mText.append(BR);
				i++;
			} else if (rbuf[i] == 0x0D) {
				// case of rbuf[last] == 0x0D and rbuf[0] == 0x0A
				lastDataIs0x0D = true;
			} else if (lastDataIs0x0D && (rbuf[0] == 0x0A)) {
				mText.append(BR);
				lastDataIs0x0D = false;
			} else if (lastDataIs0x0D && (i != 0)) {
				// only disable flag
				lastDataIs0x0D = false;
				i--;
			} else {
				mText.append((char) rbuf[i]);
			}
		}
	}

	private void closeUsbSerial() {
		mSerial.close();
	}

	protected void onNewIntent(Intent intent) {
		openUsbSerial();
	};

	private void detachedUi() {
		enableButtons(false);
	}

	private String changeEscapeSequence(String in) {
		String out = new String();
		try {
			out = unescapeJava(in);
		} catch (IOException e) {
			return "";
		}

		out = out + stTransmit;
		return out;
	}

	private void startSensor() {
		openUsbSerial();
		if (mSerial.isOpened()) {
			String strWrite = changeEscapeSequence(stStartCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			sensorData.initData();
			count = 0;
		} else {
		}
	}

	private void stopSensor() {
		if (mSerial.isOpened()) {
			String strWrite = changeEscapeSequence(stStopCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			sensorData.initData();
		} else {
		}
		closeUsbSerial();
	}

	private void setSensor(int id) {
		if (id == R.id.rd_nt) {
			mRenderer.setYTitle("Level[nT]");
		} else if (id == R.id.rd_ut) {
			mRenderer.setYTitle("Level[uT]");
		}
	}

	private void setOffset() {
		double offset = sensorData.setOffset();
		Toast.makeText(this, "Offset=" + String.valueOf(offset) + "[V]",
				Toast.LENGTH_SHORT).show();
	}

	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				if (!mSerial.isOpened()) {
					openUsbSerial();
				}
				if (!mRunningMainLoop) {
					mainloop();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				detachedUi();
				mSerial.close();
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					if (!mSerial.isOpened()) {
						openUsbSerial();
					}
				}
				if (!mRunningMainLoop) {
					mainloop();
				}
			}
		}
	};


	/**
	 * <p>
	 * Unescapes any Java literals found in the <code>String</code> to a
	 * <code>Writer</code>.
	 * </p>
	 *
	 * <p>
	 * For example, it will turn a sequence of <code>'\'</code> and
	 * <code>'n'</code> into a newline character, unless the <code>'\'</code> is
	 * preceded by another <code>'\'</code>.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> string input has no effect.
	 * </p>
	 *
	 * @param out
	 *            the <code>String</code> used to output unescaped characters
	 * @param str
	 *            the <code>String</code> to unescape, may be null
	 * @throws IllegalArgumentException
	 *             if the Writer is <code>null</code>
	 * @throws IOException
	 *             if error occurs on underlying Writer
	 */
	private String unescapeJava(String str) throws IOException {
		if (str == null) {
			return "";
		}
		int sz = str.length();
		StringBuffer unicode = new StringBuffer(4);

		StringBuilder strout = new StringBuilder();
		boolean hadSlash = false;
		boolean inUnicode = false;
		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow
				unicode.append(ch);
				if (unicode.length() == 4) {
					// unicode now contains the four hex digits
					// which represents our unicode character
					try {
						int value = Integer.parseInt(unicode.toString(), 16);
						strout.append((char) value);
						unicode.setLength(0);
						inUnicode = false;
						hadSlash = false;
					} catch (NumberFormatException nfe) {
						// throw new
						// NestableRuntimeException("Unable to parse unicode value: "
						// + unicode, nfe);
						throw new IOException("Unable to parse unicode value: "
								+ unicode, nfe);
					}
				}
				continue;
			}
			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case '\\':
					strout.append('\\');
					break;
				case '\'':
					strout.append('\'');
					break;
				case '\"':
					strout.append('"');
					break;
				case 'r':
					strout.append('\r');
					break;
				case 'f':
					strout.append('\f');
					break;
				case 't':
					strout.append('\t');
					break;
				case 'n':
					strout.append('\n');
					break;
				case 'b':
					strout.append('\b');
					break;
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;
				}
				default:
					strout.append(ch);
					break;
				}
				continue;
			} else if (ch == '\\') {
				hadSlash = true;
				continue;
			}
			strout.append(ch);
		}
		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			strout.append('\\');
		}
		return new String(strout.toString());
	}
	private enum MenuId {
		DISPLAY_TIME,
		DISPLAY_TIME_1SEC,
		DISPLAY_TIME_2SEC,
		DISPLAY_TIME_3SEC,
		DISPLAY_TIME_4SEC,
		DISPLAY_TIME_5SEC,
		DISPLAY_TIME_10SEC,
		SENSOR_TYPE,
		SENSOR_TYPE_NT,
		SENSOR_TYPE_UT,
		SET_OFFSET,
		ABOUT;
	}
	public static <E extends Enum<E>> E fromOrdinal(Class<E> enumClass, int ordinal) {
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
		AlertDialog.Builder builder;
		AlertDialog alert;
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
			sensorData.setMaxTime(time);
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
			builder = new AlertDialog.Builder(NTSensorActivity.this);
			builder.setTitle("About");
			builder.setMessage("Nano/Micro Tesla Sensor Application\n"
					+ "Provider: Aichi Steel Corporation\n"
					+ "Developer: Masahiko Fukasawa\n"
					+ "Uses: Physicaloid Library\n"
					+ "Uses: Achartengine 1.1.0\n");
			alert = builder.create();
			alert.show();
			break;

		}
		if (graphicalView != null) {
			graphicalView.repaint();
		}
		return false;
	}
}
