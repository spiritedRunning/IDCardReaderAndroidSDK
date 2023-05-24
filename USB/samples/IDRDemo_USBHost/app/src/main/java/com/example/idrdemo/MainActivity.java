package com.example.idrdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.idrdemo.ZKUSBManager.ZKUSBManager;
import com.example.idrdemo.ZKUSBManager.ZKUSBManagerListener;
import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.IDCardType;
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;
import com.zkteco.android.biometric.module.idcard.meta.IDPRPCardInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ZKUSBManager zkusbManager = null;
    // ui control
    private TextView textENNameTitle = null;
    private TextView textENNameContent = null;
    private TextView textAddrTitle = null;
    private TextView textAddrContet = null;
    private TextView textPassNoTitle = null;
    private TextView textPassNoContent = null;
    private TextView textVisaTitle = null;
    private TextView textVisaContent = null;

    private ImageView imgPhoto = null;
    private CheckBox checkBoxRepeatRead = null;
    private TextView textResult = null;
    private TextView textNameContent = null;
    private TextView textSexContent = null;
    private TextView textNationContent = null;
    private TextView textBornContent = null;
    private TextView textLicIDContent = null;
    private TextView textDepartContent = null;
    private TextView textExpireDateContent = null;

    private static final int VID = 1024;    //IDR VID
    private static final int PID = 50010;     //IDR PID
    private IDCardReader idCardReader = null;
    private boolean bStarted = false;
    private boolean bCancel = false;
    private boolean bRepeatRead = false;
    private CountDownLatch countDownLatch = null;

    private ZKUSBManagerListener zkusbManagerListener = new ZKUSBManagerListener() {
        @Override
        public void onCheckPermission(int result) {
            openDevice();
        }

        @Override
        public void onUSBArrived(UsbDevice device) {
            setResult("发现阅读器接入");
        }

        @Override
        public void onUSBRemoved(UsbDevice device) {
            setResult("阅读器USB被拔出");
        }
    };

    private void updateUIByCardType(final int cardType)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (IDCardType.TYPE_CARD_GAT == cardType) {
                    textENNameTitle.setVisibility(View.INVISIBLE);
                    textENNameContent.setVisibility(View.INVISIBLE);
                    textAddrTitle.setVisibility(View.INVISIBLE);
                    textAddrContet.setVisibility(View.INVISIBLE);
                    textPassNoTitle.setVisibility(View.VISIBLE);
                    textPassNoContent.setVisibility(View.VISIBLE);
                    textVisaTitle.setVisibility(View.VISIBLE);
                    textVisaContent.setVisibility(View.VISIBLE);
                } else if (IDCardType.TYPE_CARD_PRP == cardType) {
                    textENNameTitle.setVisibility(View.VISIBLE);
                    textENNameContent.setVisibility(View.VISIBLE);
                    textAddrTitle.setVisibility(View.INVISIBLE);
                    textAddrContet.setVisibility(View.INVISIBLE);
                    textPassNoTitle.setVisibility(View.INVISIBLE);
                    textPassNoContent.setVisibility(View.INVISIBLE);
                    textVisaTitle.setVisibility(View.INVISIBLE);
                    textVisaContent.setVisibility(View.INVISIBLE);
                } else {
                    textENNameTitle.setVisibility(View.INVISIBLE);
                    textENNameContent.setVisibility(View.INVISIBLE);
                    textAddrTitle.setVisibility(View.VISIBLE);
                    textAddrContet.setVisibility(View.VISIBLE);
                    textPassNoTitle.setVisibility(View.INVISIBLE);
                    textPassNoContent.setVisibility(View.INVISIBLE);
                    textVisaTitle.setVisibility(View.INVISIBLE);
                    textVisaContent.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void initUI()
    {
        textENNameTitle = (TextView)findViewById(R.id.textENNameTitle);
        textENNameContent = (TextView)findViewById(R.id.textENNameContent);
        textAddrTitle = (TextView)findViewById(R.id.textAddrTitle);
        textAddrContet = (TextView)findViewById(R.id.textAddrContent);
        textPassNoTitle = (TextView)findViewById(R.id.textPassNoTitle);
        textPassNoContent = (TextView)findViewById(R.id.textPassNoContent);
        textVisaTitle = (TextView)findViewById(R.id.textVisaTimesTitle);
        textVisaContent = (TextView)findViewById(R.id.textVisaTimesContent);

        imgPhoto = (ImageView)findViewById(R.id.imgPhoto);
        checkBoxRepeatRead = (CheckBox)findViewById(R.id.checkboxRepeatRead);
        textResult = (TextView)findViewById(R.id.textResult);
        textNameContent = (TextView)findViewById(R.id.textNameContent);
        textSexContent = (TextView)findViewById(R.id.textSexContent);
        textNationContent = (TextView)findViewById(R.id.textNationContent);
        textBornContent = (TextView)findViewById(R.id.textBornContent);
        textLicIDContent = (TextView)findViewById(R.id.textLicIDContent);
        textDepartContent = (TextView)findViewById(R.id.textDepartContent);
        textExpireDateContent = (TextView)findViewById(R.id.textExpireDateContent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();

        zkusbManager = new ZKUSBManager(getApplicationContext(), zkusbManagerListener);
        zkusbManager.registerUSBPermissionReceiver();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        closeDevice();
        zkusbManager.unRegisterUSBPermissionReceiver();
    }


    private boolean enumSensor()
    {
        UsbManager usbManager = (UsbManager)this.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            int device_vid = device.getVendorId();
            int device_pid = device.getProductId();
            if (device_vid == VID && device_pid == PID)
            {
                return true;
            }
        }
        return false;
    }

    private void setResult(String result)
    {
        final String mStrText = result;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textResult.setText(mStrText);
            }
        });
    }

    private void tryGetUSBPermission() {
        zkusbManager.initUSBPermission(VID, PID);
    }

    private void afterGetUsbPermission()
    {
        openDevice();
    }

    private void startIDCardReader() {
        if (null != idCardReader)
        {
            IDCardReaderFactory.destroy(idCardReader);
            idCardReader = null;
        }
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE);
        // Start fingerprint sensor
        Map idrparams = new HashMap();
        idrparams.put(ParameterHelper.PARAM_KEY_VID, VID);
        idrparams.put(ParameterHelper.PARAM_KEY_PID, PID);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.USB, idrparams);
    }



    private void openDevice()
    {
        startIDCardReader();
        try {
            idCardReader.open(0);
            countDownLatch = new CountDownLatch(1);
            new Thread(new Runnable() {
                public void run() {
                    bCancel = false;
                    while (!bCancel) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        boolean ret = false;
                        final long nTickstart = System.currentTimeMillis();
                        try {
                            idCardReader.findCard(0);
                            idCardReader.selectCard(0);
                        }catch (IDCardReaderException e)
                        {
                            if (!bRepeatRead)
                            {
                                continue;
                            }
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int cardType = 0;
                        try {
                            cardType = idCardReader.readCardEx(0, 0);
                        }
                        catch (IDCardReaderException e)
                        {
                            setResult("读卡失败，错误信息：" + e.getMessage());
                            continue;
                        }
                        if (cardType == IDCardType.TYPE_CARD_SFZ || cardType == IDCardType.TYPE_CARD_PRP || cardType == IDCardType.TYPE_CARD_GAT)
                        {
                            final long nTickCommuUsed = (System.currentTimeMillis()-nTickstart);
                            updateUIByCardType(cardType);
                            if (cardType == IDCardType.TYPE_CARD_SFZ || cardType == IDCardType.TYPE_CARD_GAT)
                            {
                                IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                                final String name = idCardInfo.getName();
                                final String sex = idCardInfo.getSex();
                                final String nation = idCardInfo.getNation();
                                final String born = idCardInfo.getBirth();
                                final String licid = idCardInfo.getId();
                                final String depart = idCardInfo.getDepart();
                                final String expireDate = idCardInfo.getValidityTime();
                                final String addr = idCardInfo.getAddress();
                                final String passNo = idCardInfo.getPassNum();
                                final int visaTimes = idCardInfo.getVisaTimes();
                                Bitmap bmpPhoto = null;
                                if (idCardInfo.getPhotolength() > 0) {
                                    byte[] buf = new byte[WLTService.imgLength];
                                    if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                                        bmpPhoto = IDPhotoHelper.Bgr2Bitmap(buf);
                                    }
                                }
                                final int final_cardType = cardType;
                                final Bitmap final_bmpPhoto = bmpPhoto;
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        imgPhoto.setImageBitmap(final_bmpPhoto);
                                        setResult("读卡成功，通讯耗时(ms)：" + nTickCommuUsed);
                                        textNameContent.setText(name);
                                        textSexContent.setText(sex);
                                        textBornContent.setText(born);
                                        textLicIDContent.setText(licid);
                                        textDepartContent.setText(depart);
                                        textExpireDateContent.setText(expireDate);
                                        if (final_cardType == IDCardType.TYPE_CARD_SFZ) {
                                            textNationContent.setText(nation);
                                            textAddrContet.setText(addr);
                                        } else {
                                            textPassNoContent.setText(passNo);
                                            textVisaContent.setText(String.valueOf(visaTimes));
                                        }
                                    }
                                });
                            }
                            else
                            {
                                IDPRPCardInfo idprpCardInfo = idCardReader.getLastPRPIDCardInfo();
                                final String cnName = idprpCardInfo.getCnName();
                                final String enName = idprpCardInfo.getEnName();
                                final String sex = idprpCardInfo.getSex();
                                final String country = idprpCardInfo.getCountry() + "/" + idprpCardInfo.getCountryCode();//国家/国家地区代码
                                final String born = idprpCardInfo.getBirth();
                                final String licid = idprpCardInfo.getId();
                                final String expireDate = idprpCardInfo.getValidityTime();
                                final String depart = "公安部";
                                Bitmap bmpPhoto = null;
                                if (idprpCardInfo.getPhotolength() > 0) {
                                    byte[] buf = new byte[WLTService.imgLength];
                                    if (1 == WLTService.wlt2Bmp(idprpCardInfo.getPhoto(), buf)) {
                                        bmpPhoto = IDPhotoHelper.Bgr2Bitmap(buf);
                                    }
                                }
                                final int final_cardType = cardType;
                                final Bitmap final_bmpPhoto = bmpPhoto;
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        imgPhoto.setImageBitmap(final_bmpPhoto);
                                        setResult("读卡成功，通讯耗时(ms)：" + nTickCommuUsed);
                                        textNameContent.setText(cnName);
                                        textENNameContent.setText(enName);
                                        textSexContent.setText(sex);
                                        textNationContent.setText(country);
                                        textBornContent.setText(born);
                                        textLicIDContent.setText(licid);
                                        textDepartContent.setText(depart);
                                        textExpireDateContent.setText(expireDate);
                                    }
                                });
                            }
                        }
                    }
                    countDownLatch.countDown();
                }
            }).start();
            bStarted = true;
            setResult("打开设备成功，SAMID:" + idCardReader.getSAMID(0));
        } catch (IDCardReaderException e) {
            e.printStackTrace();
            setResult("打开设备失败");
        }

    }

    private void closeDevice()
    {
        if (bStarted)
        {
            bCancel = true;
            if (null != countDownLatch)
            {
                try {
                    countDownLatch.await(2*1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countDownLatch = null;
            }
            try {
                idCardReader.close(0);
            } catch (IDCardReaderException e) {
                e.printStackTrace();
            }
            bStarted = false;
        }
    }

    public void onBnStart(View view)
    {
        if (!enumSensor())
        {
            setResult("找不到设备");
            return;
        }
        bRepeatRead = checkBoxRepeatRead.isChecked();
        tryGetUSBPermission();
    }

    public void onBnStop(View view)
    {
        closeDevice();
        setResult("设备断开连接");
    }
}