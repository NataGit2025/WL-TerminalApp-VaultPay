package com.VaultPay.demoui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.dspread.xpos.CQPOSService;
import com.VaultPay.demoui.R;
import com.VaultPay.demoui.utils.QPOSStatus;
import com.VaultPay.demoui.utils.TRACE;

public class WMX_Transaction_Cancel extends BaseActivity {
    private String  Amount, AmountToShow, type_transaction, ksn_posId, _Propina,_noAuth, total, months_total, subtotal, tips, msi, approve,tarjeta;
    private Button btn_retry, btn_cancel;
    private TextView txt_prosaerror;
    private Intent intent;

    @Override
    public void onBackPressed() {

    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setProps();
        btn_retry = findViewById(R.id.btn_retry);
        btn_cancel = findViewById(R.id.btn_cancel);
        txt_prosaerror = (TextView) findViewById(R.id.txtprosaerror);

        btn_retry.setOnClickListener(this::onRetry);
        btn_cancel.setOnClickListener(this::onCancel);
        onEnableRetry(false);

        QPOSStatus.getInstance().addActivityListeners("cancelTransaction", new CQPOSService() {
            @Override
            public void onRequestQposDisconnected() {
                TRACE.d("CANCEL DISABLED");
                runOnUiThread(() -> {
                    onEnableRetry(true);
                });
            }
        });
    }

    private void onEnableRetry(boolean enable) {
        btn_retry.setEnabled(enable);
        btn_retry.getBackground().setAlpha(enable ? 255 : 160);
        btn_cancel.setEnabled(enable);
        btn_cancel.getBackground().setAlpha(enable ? 255 : 160);
    }

    @Override
    public void onStart() {
        super.onStart();
        String ErrorMessage = intent.getStringExtra("error");
        if(!TextUtils.isEmpty(ErrorMessage)) {
            WMX_Transaction_Cancel.super.showAlert("ERROR", ErrorMessage);
            txt_prosaerror.setText(ErrorMessage);
        }
    }

    private void onRetry(View view) {
        Intent intent = new Intent(WMX_Transaction_Cancel.this, WMX_Card.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("Amount", Amount);
        intent.putExtra("AmountToShow", AmountToShow);
        intent.putExtra("type_transaction", type_transaction);
        intent.putExtra("ksn_posId", ksn_posId);
        intent.putExtra("propina", _Propina);
        intent.putExtra("months", msi);
        intent.putExtra("cp_tv_auth", _noAuth);
        intent.putExtra("total", total);
        intent.putExtra("months_total",  months_total);
        intent.putExtra("subtotal", subtotal);
        intent.putExtra("tips", tips);
        intent.putExtra("approve", approve);
        intent.putExtra("tarjeta", tarjeta);
        startActivity(intent);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void onCancel(View view) {
        new Handler().postDelayed(() -> {
            finishAffinity();
        }, 1000);
        startActivity(new Intent(this, WMX_Menu.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private void setProps() {
        intent = getIntent();
        Amount = intent.getStringExtra("Amount");
        AmountToShow = intent.getStringExtra("AmountToShow");
        type_transaction = intent.getStringExtra("type_transaction");
        ksn_posId = intent.getStringExtra("ksn_posId");
        _Propina=intent.getStringExtra("propina");
        msi= intent.getStringExtra("months");
        _noAuth=intent.getStringExtra("cp_tv_auth");
        total=intent.getStringExtra("total");
        months_total = intent.getStringExtra("months_total");
        subtotal = intent.getStringExtra("subtotal");
        tips =  intent.getStringExtra("tips");
        approve =  intent.getStringExtra("approve");
        tarjeta=intent.getStringExtra("tarjeta");
    }

    @Override
    public void onToolbarLinstener() {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.wmn_cancelation_trade;
    }
}
