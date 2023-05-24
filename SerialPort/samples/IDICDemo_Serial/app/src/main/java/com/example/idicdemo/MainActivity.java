package com.example.idicdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.WLTService;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.core.utils.ToolUtils;
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
    private TextView textResult = null;
    private TextView textNameContent = null;
    private TextView textSexContent = null;
    private TextView textNationContent = null;
    private TextView textBornContent = null;
    private TextView textLicIDContent = null;
    private TextView textDepartContent = null;
    private TextView textExpireDateContent = null;

	private EditText editSerialName = null;
    private EditText editBlockAddr = null;
    private EditText editSectorKey = null;
    private EditText editBlockData = null;

    private IDCardReader idCardReader = null;
    private boolean bStarted = false;
    private boolean bCancel = false;
    private boolean bRepeatRead = false;
    private CountDownLatch countDownLatch = null;
    private String serialName = "";

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
        textResult = (TextView)findViewById(R.id.textResult);
        textNameContent = (TextView)findViewById(R.id.textNameContent);
        textSexContent = (TextView)findViewById(R.id.textSexContent);
        textNationContent = (TextView)findViewById(R.id.textNationContent);
        textBornContent = (TextView)findViewById(R.id.textBornContent);
        textLicIDContent = (TextView)findViewById(R.id.textLicIDContent);
        textDepartContent = (TextView)findViewById(R.id.textDepartContent);
        textExpireDateContent = (TextView)findViewById(R.id.textExpireDateContent);
		
		editSerialName = (EditText)findViewById(R.id.editSerialName);
        editBlockAddr = (EditText)findViewById(R.id.editBlockAddr);
        editSectorKey = (EditText)findViewById(R.id.editKeyData);
        editBlockData = (EditText)findViewById(R.id.editBlockData);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        closeDevice();
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
        idrparams.put(ParameterHelper.PARAM_SERIAL_SERIALNAME, serialName);
        idrparams.put(ParameterHelper.PARAM_SERIAL_BAUDRATE, 115200);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.SERIALPORT, idrparams);
    }



    private void openDevice()
    {
        startIDCardReader();
        try {
            idCardReader.open(0);
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
            try {
                idCardReader.close(0);
            } catch (IDCardReaderException e) {
                e.printStackTrace();
            }
            bStarted = false;
        }
    }

    public void onBnConnect(View view)
    {
        serialName = editSerialName.getText().toString();
        if (null == serialName || serialName.isEmpty())
        {
            setResult("请输入串口名！");
            return;
        }
        openDevice();
    }

    public void onBnDisconnect(View view)
    {
        closeDevice();
        setResult("设备断开连接");
    }
	
	public void onBnReadIDCard(View view)
    {
        if (!bStarted)
        {
            setResult("设备未连接");
            return;
        }
        long nTickstart = System.currentTimeMillis();
        try {
            idCardReader.findCard(0);
            idCardReader.selectCard(0);
        }catch (IDCardReaderException e)
        {
            //setResult("寻卡/选卡失败，错误信息：" + e.getMessage());
            //return;
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
            return;
        }
        if (cardType == IDCardType.TYPE_CARD_SFZ || cardType == IDCardType.TYPE_CARD_PRP || cardType == IDCardType.TYPE_CARD_GAT)
        {
            long nTickCommuUsed = (System.currentTimeMillis()-nTickstart);
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
                imgPhoto.setImageBitmap(bmpPhoto);
                setResult("读卡成功，通讯耗时(ms)：" + nTickCommuUsed);
                textNameContent.setText(name);
                textSexContent.setText(sex);
                textBornContent.setText(born);
                textLicIDContent.setText(licid);
                textDepartContent.setText(depart);
                textExpireDateContent.setText(expireDate);
                if (cardType == IDCardType.TYPE_CARD_SFZ) {
                    textNationContent.setText(nation);
                    textAddrContet.setText(addr);
                } else {
                    textPassNoContent.setText(passNo);
                    textVisaContent.setText(String.valueOf(visaTimes));
                }
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
                imgPhoto.setImageBitmap(bmpPhoto);
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
        }
        else {
            setResult("读卡失败");
        }
    }

    // byte[]转字符串，去除0x0
    private static String newStringBytes(byte[] stringBytes) {
        int nLen = stringBytes.length;

        for(int i = 0; i < nLen; ++i) {
            if (stringBytes[i] == 0) {
                nLen = i;
                break;
            }
        }

        return new String(stringBytes, 0, nLen).trim();
    }

    public void onBnGetFirmwareversion(View view)
    {
        if (!bStarted)
        {
            setResult("设备未连接");
            return;
        }
        byte[] firmwareVersion = new byte[64];
        try {
            boolean ret = idCardReader.Get_VersionNum(0, firmwareVersion);
            if (ret)
            {
                setResult("固件版本号：" + newStringBytes(firmwareVersion));
                return;
            }
            else
            {
                setResult("读取固件版本失败！");
            }
        } catch (IDCardReaderException e) {
            e.printStackTrace();
            setResult("读取固件版本失败，错误信息：" + e.getMessage());
        }
    }

    public void onBnGetICSnr(View view)
    {
        if (!bStarted)
        {
            setResult("设备未连接");
            return;
        }
        byte mode = 0x26;
        byte halt = 0x0;
        try {
            String snrHex = idCardReader.MF_GET_SNR_HEX(0, mode, halt);
            setResult("IC卡物理卡号（HEX）：" + snrHex);
        } catch (IDCardReaderException e) {
            e.printStackTrace();
            setResult("读取IC卡物理卡号失败，错误信息：" + e.getMessage());
        }
    }

    public void onBnGetIDSnr(View view)
    {
        if (!bStarted)
        {
            setResult("设备未连接");
            return;
        }
        byte mode = 0x26;
        byte halt = 0x0;
        byte[] idsnrBuf = new byte[64];
        try {
            boolean ret = idCardReader.MF_GET_NIDCardNum(0, mode, halt, idsnrBuf);
            if (ret)
            {
                //身份证物理卡号8字节
                setResult("身份证物理卡号（HEX）：" + ToolUtils.bytesToHexString(idsnrBuf, 0, 8));
            }
            else
            {
                setResult("读取身份证物理卡号失败!");
            }
        } catch (IDCardReaderException e) {
            e.printStackTrace();
            setResult("读取身份证物理卡号失败，错误信息：" + e.getMessage());
        }
    }


    /**
     * hex转byte数组
     * @param hex
     * @return
     */
    public static byte[] hexToByte(String hex){
        int m = 0, n = 0;
        int byteLen = hex.length() / 2; // 每两个字符描述一个字节
        byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            int intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n));
            ret[i] = Byte.valueOf((byte)intVal);
        }
        return ret;
    }


    public void onBnReadICCard(View view)
    {
        if (!bStarted)
        {
            setResult("设备未连接");
            return;
        }
        String strBlockAddr = editBlockAddr.getText().toString();
        String strSectorKey = editSectorKey.getText().toString();
        if (strSectorKey == null || strSectorKey.length() != 12 ||
        null == strBlockAddr)
        {
            setResult("请输入块地址和扇区秘钥");
            return;
        }
        byte blockAddr = (byte) Integer.parseInt(strBlockAddr);
        if (blockAddr < 0 || blockAddr >= 64)
        {
            setResult("请输入有效块地址(S50卡只有64个块，块地址取值范围0~63)！");
            return;
        }
        byte[] key = hexToByte(strSectorKey);
        byte[] cardNum = new byte[4];
        byte[] cardData = new byte[16];
        byte mode = 0x1;
        byte blockCount = 1;
        try {
            boolean ret = idCardReader.MF_Read(0, mode, blockCount, blockAddr, key, cardNum, cardData);
            if (ret)
            {
                setResult("读取成功，内容：" + ToolUtils.bytesToHexString(cardData));
            }
            else
            {
                setResult("读取IC卡失败！");
            }
        } catch (IDCardReaderException e) {
            e.printStackTrace();
            setResult("读取IC卡失败，错误信息：" + e.getMessage());
        }
    }

    public void onBnWriteICCard(View view)
    {
        if (!bStarted)
        {
            setResult("设备未连接");
            return;
        }
        String strBlockAddr = editBlockAddr.getText().toString();
        String strSectorKey = editSectorKey.getText().toString();
        String strBlockData = editBlockData.getText().toString();
        if (strSectorKey == null || strSectorKey.length() != 12 ||
                strBlockData == null || strBlockData.length() != 32 ||
                null == strBlockAddr)
        {
            setResult("请输入块地址,扇区秘钥,块数据！");
            return;
        }
        byte blockAddr = (byte) Integer.parseInt(strBlockAddr);
        if (blockAddr <= 0 || blockAddr >= 64 || (blockAddr+1)%4 == 0)
        {
            setResult("请输入有效块地址(S50卡只有64个块，块地址取值范围0~63), 0块,每个扇区第四块不允许写入！");
            return;
        }
        byte[] key = hexToByte(strSectorKey);
        byte[] data = hexToByte(strBlockData);
        byte[] cardNum = new byte[4];
        byte mode = 0x1;
        byte blockCount = 1;
        try {
            boolean ret = idCardReader.MF_Write(0, mode, blockCount, blockAddr, key, data, cardNum);
            if (ret)
            {
                setResult("写卡成功");
            }
            else
            {
                setResult("写卡失败");
            }
        } catch (IDCardReaderException e) {
            e.printStackTrace();
            setResult("写卡失败，错误信息：" + e.getMessage());
        }
    }
}