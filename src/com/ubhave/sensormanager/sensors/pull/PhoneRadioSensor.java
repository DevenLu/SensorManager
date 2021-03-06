/* **************************************************
 Copyright (c) 2014, Idiap
 Olivier Bornet, olivier.bornet@idiap.ch

This file was developed to add phone radio sensor to the SensorManager library
from https://github.com/nlathia/SensorManager.

The SensorManager library was developed as part of the EPSRC Ubhave (Ubiquitous
and Social Computing for Positive Behaviour Change) Project. For more
information, please visit http://www.emotionsense.org

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 ************************************************** */

package com.ubhave.sensormanager.sensors.pull;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.data.pullsensor.PhoneRadioData;
import com.ubhave.sensormanager.data.pullsensor.PhoneRadioDataList;
import com.ubhave.sensormanager.process.pull.PhoneRadioProcessor;
import com.ubhave.sensormanager.sensors.SensorUtils;

public class PhoneRadioSensor extends AbstractPullSensor
{
	private static final String TAG = "PhoneRadioSensor";
	private static final String PERMISSION_ACCESS_CELL_INFO = "android.permission.ACCESS_COARSE_LOCATION";

	private static PhoneRadioSensor phoneRadioSensor;
	private static Object lock = new Object();

	private ArrayList<PhoneRadioData> visibleCells;
	private volatile PhoneRadioDataList phoneRadioDataList;

	public static PhoneRadioSensor getPhoneRadioSensor(final Context context)
			throws ESException
	{
		if (phoneRadioSensor == null)
		{
			synchronized (lock)
			{
				if (phoneRadioSensor == null)
				{
					if (permissionGranted(context, PERMISSION_ACCESS_CELL_INFO))
					{
						phoneRadioSensor = new PhoneRadioSensor(context);
					}
					else
					{
						throw new ESException(ESException.PERMISSION_DENIED,
								SensorUtils.SENSOR_NAME_PHONE_RADIO);
					}
				}
			}
		}
		return phoneRadioSensor;
	}

	private PhoneRadioSensor(Context context)
	{
		super(context);
	}

	protected String getLogTag()
	{
		return TAG;
	}

	public int getSensorType()
	{
		return SensorUtils.SENSOR_TYPE_PHONE_RADIO;
	}

	protected PhoneRadioDataList getMostRecentRawData()
	{
		return phoneRadioDataList;
	}

	protected void processSensorData()
	{
		PhoneRadioProcessor processor = (PhoneRadioProcessor) getProcessor();
		phoneRadioDataList = processor.process(pullSenseStartTimestamp,
				visibleCells, sensorConfig.clone());
	}

	protected boolean startSensing()
	{
		new Thread()
		{
			@SuppressLint("NewApi")
			public void run()
			{
				try
				{
					visibleCells = new ArrayList<PhoneRadioData>();
					TelephonyManager telephonyManager = (TelephonyManager) applicationContext
							.getSystemService(Context.TELEPHONY_SERVICE);
					List<CellInfo> cellInfos = telephonyManager
							.getAllCellInfo();
					if (cellInfos == null)
					{
						// getAllCellInfo() not supported, try old methods
						switch (telephonyManager.getPhoneType())
						{
						case TelephonyManager.PHONE_TYPE_GSM:
							GsmCellLocation cellLocation = (GsmCellLocation) telephonyManager
									.getCellLocation();
							String networkOperator = telephonyManager
									.getNetworkOperator();
							String mcc = networkOperator.substring(0, 3);
							String mnc = networkOperator.substring(3);
							visibleCells.add(new PhoneRadioData(mcc, mnc,
									cellLocation.getLac(), cellLocation
											.getCid()));
							break;
						default:
							// TODO: handle unsupported phone type...
							break;
						}
					} else {
						// TODO: handle getAllCellInfo() values...
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				} finally
				{
					// sensing complete
					notifySenseCyclesComplete();
				}
			}
		}.start();

		return true;
	}

	// Called when a scan is finished
	protected void stopSensing()
	{
	}

}
