package com.ly.createaar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.fastble.fastble.data.BleDevice;
import com.ly.qcommesim.core.widget.LoadingWidget;
import com.ly.qcommesim.core.widget.ProgressDialogWidget;
import com.ly.qcommesim.qcomm.annotation.ConfirmationType;
import com.ly.qcommesim.qcomm.annotation.Enums;
import com.ly.qcommesim.qcomm.annotation.ErrorTypes;
import com.ly.qcommesim.qcomm.helper.QualcommHelper;
import com.ly.qcommesim.qcomm.service.QcommBleService;
import com.ly.qcommesim.qcomm.upgrade.UpgradeError;
import com.ly.qcommesim.qcomm.widget.VMUpgradeDialog;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class QcommUpgradeActivity1 extends FragmentActivity implements VMUpgradeDialog.UpgradeDialogListener {

    private BleDevice bleDevice;
    private EditText macEt;
    private QcommBleService otaUpgradeService1;
    private DecimalFormat decimalFormat;
//    private VMUpgradeDialog vmUpgradeDialog;
    private List<String> fileNameList = new ArrayList<>();
    private Button gtBtn;
    private boolean isBond = false;
    private String testMac = "88:9e:33:ee:a7:93";
    private QualcommHelper qualcommHelper;
    private AlertDialog mDialogReconnection;
    private TextView showTxt;
    private String filePath;
    private String mUrl;
    private boolean startUpgrade=false;
    private ProgressDialogWidget progressDialogWidget;
    private LoadingWidget loadingWidget;

    /**
     * <p>This method allows the Upgrade process to be able to ask the user any confirmation to be able to carry on
     * the upgrade process.</p>
     * 该方法允许升级过程能够要求用户进行任何确认才能进行升级过程。
     *
     * @param confirmation The type of confirmation which has to be asked.
     */
    private void askForConfirmation1(@ConfirmationType final int confirmation) {
        Log.e("confirm---",confirmation+"");
        switch (confirmation) {
            case ConfirmationType.COMMIT:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_commit_title, R.string.alert_upgrade_commit_message);
                break;
            case ConfirmationType.IN_PROGRESS:
                // no obligation to ask for confirmation as the commit confirmation will happen next
                otaUpgradeService1.sendConfirmation(confirmation, true);
                break;
            case ConfirmationType.TRANSFER_COMPLETE:
                //升级包传输完成
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_transfer_complete_title,
                        R.string.alert_upgrade_transfer_complete_message);
                break;
            case ConfirmationType.BATTERY_LOW_ON_DEVICE:
                //设备电量低
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_low_battery_title,
                        R.string.alert_upgrade_low_battery_message);
                break;
            case ConfirmationType.WARNING_FILE_IS_DIFFERENT:
                displayConfirmationDialog(confirmation, R.string.alert_upgrade_sync_id_different_title,
                        R.string.alert_upgrade_sync_id_different_message);
                break;
        }
    }

    private void displayConfirmationDialog(@ConfirmationType final int confirmation, int title, int
            message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton(R.string.button_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        otaUpgradeService1.sendConfirmation(confirmation, true);
                    }
                })
                .setNegativeButton(R.string.button_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        otaUpgradeService1.sendConfirmation(confirmation, false);
//                        showUpgradeDialog(false);
                    }
                });
        builder.show();
    }

    private void initReconnectionDialog() {
        // build the dialog to show a progress bar when we try to reconnect.
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(QcommUpgradeActivity1.this);
        dialogBuilder.setTitle(getString(R.string.alert_reconnection_title));

        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") // the root can be null as "attachToRoot" is false
                View dialogLayout = inflater.inflate(R.layout.dialog_progress_bar, null, false);
        dialogBuilder.setView(dialogLayout);
        dialogBuilder.setCancelable(false);
        mDialogReconnection = dialogBuilder.create();

    }

    private void manageError1(UpgradeError error) {
        switch (error.getError()) {
            case ErrorTypes.AN_UPGRADE_IS_ALREADY_PROCESSING:
                // nothing should happen as there is already an upgrade processing.
                // in case it's not already displayed, we display the Upgrade dialog
                showUpgradeDialog(true);
                break;

            case ErrorTypes.ERROR_BOARD_NOT_READY:
                // display error message + "please try again later"
//                if (vmUpgradeDialog.isAdded()) {
//                    vmUpgradeDialog.displayError(getString(R.string.dialog_upgrade_error_board_not_ready));
//                }
                break;

            case ErrorTypes.EXCEPTION:
                // display that an error has occurred?
//                if (vmUpgradeDialog.isAdded()) {
//                    vmUpgradeDialog.displayError(getString(R.string.dialog_upgrade_error_exception));
//                }
                break;

            case ErrorTypes.NO_FILE:
                if (!startUpgrade) return;
                displayFileError();
                break;

            case ErrorTypes.RECEIVED_ERROR_FROM_BOARD:
//                if (vmUpgradeDialog.isAdded()) {
//                    vmUpgradeDialog.displayError(ReturnCodes.getReturnCodesMessage(error.getReturnCode()),
//                            Utils.getIntToHexadecimal(error.getReturnCode()));
//                }
                break;

            case ErrorTypes.WRONG_DATA_PARAMETER:
//                if (vmUpgradeDialog.isAdded()) {
//                    vmUpgradeDialog.displayError(getString(R.string.dialog_upgrade_error_protocol_exception));
//                }
                break;
        }
    }

    private void displayFileError() {
        showUpgradeDialog(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_file_error_message)
                .setTitle(R.string.alert_file_error_title)
                .setPositiveButton(R.string.button_ok, null);
        builder.show();
    }

    private void showUpgradeDialog(boolean show) {
//        if (show && !vmUpgradeDialog.isAdded()) {
//            vmUpgradeDialog.show(getFragmentManager(),getString(R.string.dialog_upgrade_title));
//        } else if (!show && vmUpgradeDialog.isAdded()) {
//            vmUpgradeDialog.dismiss();
//        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qualcomm);
        macEt = findViewById(R.id.mac_et);
        gtBtn = findViewById(R.id.gt_btn);
        progressDialogWidget = findViewById(R.id.progress_dialog);
        loadingWidget = findViewById(R.id.main_loading_widget);
        decimalFormat = new DecimalFormat("0.00");
//        combinePacket();
        initHelper();
        initReconnectionDialog();
        checkLocation();

        gtBtn.setOnClickListener(v -> {
            startUpgrade=true;

        });
    }

    private void initHelper() {
        String filePathName = Environment.getExternalStorageDirectory().getPath()
                + File.separatorChar + "test-data";
        File file1 = new File(filePathName);
        String fn;
        if (file1.exists()) {
            for (File file2 : file1.listFiles()) {
                String fileName = file2.getName();
                if (fileName.endsWith(".bin")) {
                    fn = fileName;
                    filePath=file2.getAbsolutePath();
                }
            }

        }
        qualcommHelper=new QualcommHelper(getApplication(),this,testMac,filePath) {
            @Override
            public void macInvalidate() {

            }

            @Override
            public void phoneBleDisable() {

            }

            @Override
            public void deviceNotFound() {
                Toast.makeText(QcommUpgradeActivity1.this,"device not found ",Toast.LENGTH_SHORT).show();
            }


            @Override
            public void deviceDisconnects(boolean isActiveDisConnected, BleDevice device) {
                Toast.makeText(QcommUpgradeActivity1.this,"device disconnect ",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void showProgress() {
                super.showProgress();
                loadingWidget.hide();
                progressDialogWidget.show();
            }

            @Override
            public void updateProgress(double progress) {
                super.updateProgress(progress);
                progressDialogWidget.getProgressNumTv().setText(decimalFormat.format(progress) + "%");
            }
        };
    }


    // todo
    private boolean checkLocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        0x0010
                );
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    // todo
    private void toast(String msg) {
        Toast.makeText(this, "" + msg, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    //connect to device
    public void connect(View view) {
        String macAddress = macEt.getText().toString().trim();
        if (TextUtils.isEmpty(macAddress)) {
            macAddress = testMac;
        }
        loadingWidget.show();
          qualcommHelper.start();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (otaUpgradeService1 != null) {
            qualcommHelper.stop();
        }
    }

    @Override
    public void abortUpgrade() {
        otaUpgradeService1.abortUpgrade();
    }

    @Override
    public int getResumePoint() {
        return (otaUpgradeService1 != null) ? otaUpgradeService1.getResumePoint() : Enums.DATA_TRANSFER;
    }
}
