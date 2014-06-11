package us.aichisteel.ntsensor;

import java.util.ArrayList;
import java.util.List;

public class NTSensorData {
	// public static final double UNIT_PICO_TESLA = 1000000;
	// public static final double UNIT_NANO_TESLA = 1000;
	// public static final double UNIT_MICRO_TESLA =1.0;
	// public static final double UNIT_MILI_TESLA = 0.001;
	// private double mUnit = UNIT_NANO_TESLA;

	private int mMaxSize = 1000; //
	private double mSensitivity = 4; // 4[V/uT]
	private double mOffset = 2.3;
	private List<Double> mSensorData;
	private StringBuilder mText = new StringBuilder();
	private double mLatestVoltage = mOffset;
	public static final int NTSENSOR_SPS = 250; // [SPS]

	public NTSensorData() {
		super();
		this.mSensorData = new ArrayList<Double>();
		this.mSensitivity = 4;
	}

	public void setSensitibity(double sen) {
		mSensitivity = sen;
	}

	public List<Double> getData() {
		return mSensorData;
	}

	public double setOffset() {
		mOffset = mLatestVoltage;
		return mOffset;
	}

	// public void setUnit(double unit){
	// mUnit = unit;
	// }

	public void setMaxTime(double sec) {
		if (sec > 0) {
			mMaxSize = (int) (NTSENSOR_SPS * sec);
		}
	}

	public void initData() {
		mSensorData.clear();
		mText.setLength(0);
	}

	public void addData(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			if (rbuf[i] == 'v') {
				double d;
				try {
					mLatestVoltage = Double.parseDouble(mText.toString());
					d = 1000 * (mLatestVoltage - mOffset) / mSensitivity;
				} catch (Exception e) {
					d = 0;
				}
				mSensorData.add(d);
				mText.setLength(0);

				while (mSensorData.size() > mMaxSize) {
					mSensorData.remove(0);
				}
			} else if (rbuf[i] >= '0' && rbuf[i] <= '9') {
				mText.append((char) rbuf[i]);
			} else if (rbuf[i] == '.') {
				mText.append((char) rbuf[i]);
			}
		}
	}
}
