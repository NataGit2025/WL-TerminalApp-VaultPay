package com.VaultPay.demoui.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.dspread.print.device.PrintListener;
import com.dspread.xpos.QPOSService;
import com.VaultPay.demoui.R;
import com.VaultPay.demoui.fragments.NotConnectionDialog;
import com.VaultPay.demoui.interfaces.FetchEntity;
import com.VaultPay.demoui.interfaces.IFetchs;
import com.VaultPay.demoui.interfaces.ITicket;
import com.VaultPay.demoui.interfaces.TicketLayoutType;
import com.VaultPay.demoui.utils.ActivityFlags;
import com.VaultPay.demoui.utils.FLAGS;
import com.VaultPay.demoui.utils.FetchUIManager;
import com.VaultPay.demoui.utils.PRINT_TYPE;
import com.VaultPay.demoui.utils.RequestSingleton;
import com.VaultPay.demoui.utils.StatusBarCompat;
import com.VaultPay.demoui.utils.TRACE;
import com.VaultPay.demoui.utils.Ticket;
import com.VaultPay.demoui.utils.TicketLayoutManager;
import com.VaultPay.demoui.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BaseActivity used for to build all activity
 */

public abstract class BaseActivity extends AppCompatActivity implements ITicket, IFetchs {
    public static final String TAG = "BaseActivity";

    protected Toolbar toolbar;
    private TextView txt_toolbar_title;
    private ImageView img_invisible_margin, logo_image;
    private LayoutInflater inflater;
    private LinearLayout container_logo;
    private Ticket ticket;
    private ProgressDialog ticket_progress;
    private ProgressDialog fetch_progress;
    private PRINT_TYPE entity_print;
    private TicketLayoutType ticketLayoutType;

    private FetchUIManager manager;
    protected LinearLayout toolbar_btn_calendar;
    protected Handler ticketHandler;
    protected View actionbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedInstanceState.clear();
            savedInstanceState = null;
        }
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            txt_toolbar_title = toolbar.findViewById(R.id.txt_toolbar_title);
            img_invisible_margin = toolbar.findViewById(R.id.img_invisible_margin);
            setInvisiblemargin(false);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // show the left arrow
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            setDefaultToolbarColor();
            toolbar.setPadding(0, 0, 0, 0);
            logo_image = toolbar.findViewById(R.id.toolbar_logo);
            container_logo = toolbar.findViewById(R.id.toolbar_logo_container);
            toolbar_btn_calendar = findViewById(R.id.toolbar_btn_calendar);
            actionbar = findViewById(R.id.actionbar);
            toolbar_btn_calendar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCalendarLinstener();
                }
            });
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToolbarLinstener();
                }
            });
        }

        ticket = new Ticket(this);
        ticket.setPrintListenner(new MyPrinterListener());
        ticket_progress = Utils.getLoaderSpinner(this, "Imprimiendo ticket...");
        ticketHandler = new Handler();
        entity_print = PRINT_TYPE.STORE;

        fetch_progress = Utils.getLoaderSpinner(this, "Cargando...");
        manager = new FetchUIManager(this) {
            @Override
            public void onFetchCurrentResult(FetchEntity entity, @Nullable FetchEntity error) {
                super.onFetchCurrentResult(entity, error);
                BaseActivity.this.onFetchCurrentResult(entity, error);
            }

            @Override
            public void onFetchResults(List<FetchEntity> entities, List<FetchEntity> errors) {
                super.onFetchResults(entities, errors);
                BaseActivity.this.onFetchResults(entities, errors);
            }

            @Override
            public void onRequestsFetching(boolean isFetching) {
                super.onRequestsFetching(isFetching);
                BaseActivity.this.onRequestsFetching(isFetching);
            }
        };
        try {
            addFetchs(manager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueue requestQueue = RequestSingleton.getInstance(this).getRequestQueue();
        requestQueue.getCache().clear();
    }

    protected void setFetchProgressTitle(String title) {
        if (fetch_progress == null)
            return;
        fetch_progress.setMessage(title);
    }

    public FetchUIManager getFetchManager() {
        return this.manager;
    }

    public Ticket getTicket() {
        return this.ticket;
    }

    @Override
    public TicketLayoutType getPrintLayout() {
        return TicketLayoutType.NONE;
    }

    @Override
    public void setTicketData(Ticket ticket) {
    }

    public void PrintTicket(PRINT_TYPE entity) {
        this.entity_print = entity;
        PrintTicket();
    }

    public void PrintTicket() {
        runOnUiThread(() -> {
            boolean success = false;
            if (!ticket.isPrinterAvailable())
                return;
            if(!ticket_progress.isShowing()) ticket_progress.show();
            try {
                ticketLayoutType = getPrintLayout();
                setTicketData(ticket);
                TicketLayoutManager ticketLayoutManager = new TicketLayoutManager(getLayoutInflater(), ticketLayoutType,
                        this.entity_print);
                ticketLayoutManager.setTicketDataByLayout(ticket);

                // Se agrega un posdelay en caso de que haya un error que la libreria no este
                // catcheando para ocultar el spinner
                ticketHandler.postDelayed(() -> {
                    hideTicketSpinner();
                    ticket.close();
                }, 7000);
                success = ticket.printLayout(ticketLayoutManager.getLayout());
            } catch (Exception e) {
                TRACE.d("TICKET EXCEPTION: " + e.getMessage());
                e.printStackTrace();
            }
            if (!success) {
                hideTicketSpinner();
                ticketHandler.removeCallbacksAndMessages(null);
            }
        });
    }

    public Toolbar getToolbar() {
        if (toolbar != null) {
            return this.toolbar;
        }
        return null;
    }

    @Override
    public void onPrintFinished(boolean isSuccess, PRINT_TYPE print_type, TicketLayoutType layoutType) {
    }

    @Override
    public void onPrintError(boolean isSuccess, String status, PRINT_TYPE print_type, TicketLayoutType layoutType) {
    }

    public abstract void onToolbarLinstener();

    public void onCalendarLinstener() {
    };

    protected abstract int getLayoutId();

    @SuppressLint("NewApi")
    protected String getFinalErrorMessage(String message) {
        String messageLower = message.toLowerCase(Locale.ROOT);
        Map.Entry<String, String> getMessage = Utils.errorMessagesDictionary.entrySet().stream()
                .filter(x -> messageLower.contains(x.getKey())).findAny().orElse(null);
        return getMessage != null ? getMessage.getValue() : "Ha ocurrido un error desconocido";
    }

    @SuppressLint("NewApi")
    protected String getFinalErrorMessage(QPOSService.Error status) {
        Map.Entry<QPOSService.Error, String> getMessage = Utils.errorPosDictionary.entrySet().stream()
                .filter(x -> status == x.getKey()).findAny().orElse(null);
        return getMessage != null ? getMessage.getValue() : "Ha ocurrido un error desconocido";
    }

    @Override
    public void onFetchCurrentResult(FetchEntity entity, @Nullable FetchEntity error) {
        if (error != null) {
            String message = error.result.toString().toLowerCase(Locale.ROOT);
            showAlert("error", getFinalErrorMessage(message));
        }
    }

    @Override
    public void addFetchs(FetchUIManager manager) throws Exception {

    }

    @Override
    public void onFetchResults(List<FetchEntity> entities, List<FetchEntity> errors) {

    }

    @Override
    public void onRequestsFetching(boolean isFetching) {
        if (isFetching) {
            fetch_progress.show();
        } else if (fetch_progress.isShowing()) {
            fetch_progress.dismiss();
        }
    }

    // protected abstract int getFragmentContainer();

    public void setActionBarIcon(int iconRes) {
        toolbar.setNavigationIcon(iconRes);
    }

    public void setTitle(int titleResource) {
        setTitle(getResources().getString(titleResource));
    }

    public void setTitle(String title) {
        if (title != null && !title.equals("")) {
            if (txt_toolbar_title != null) {
                txt_toolbar_title.setText(title);
                toolbar.setTitle("");
            } else if (toolbar != null) {
                toolbar.setTitle(title);
                txt_toolbar_title.setText("");
            }
        }
    }

    public void setDefaultToolbarColor() {
        setToolbarBgColor(ContextCompat.getColor(this, R.color.ep_fondo));
        // setToolbarTextColor(ContextCompat.getColor(this,R.color.wmx_purble));
        setToolbarIconColor(ContextCompat.getColor(this, R.color.ep_icon_back));
        setStatusBarColor(ContextCompat.getColor(this, R.color.eb_col_11));
        setTitle("Wirebit MX");
    }

    public void setCustomToolbarColor(int CustomColor) {
        // set toolbar bg color
        setToolbarBgColor(CustomColor);
        // set toolbar text color
        int midColor = getResources().getColor(R.color.custom_middle_color);
        if (CustomColor >= midColor) {
            // setToolbarTextColor(getResources().getColor(R.color.custom_dark_color));
            setToolbarIconColor(getResources().getColor(R.color.custom_dark_color));
        } else {
            // setToolbarTextColor(getResources().getColor(R.color.custom_light_color));
            setToolbarIconColor(getResources().getColor(R.color.custom_light_color));
        }
        setStatusBarColor(CustomColor);
        setToolbarIconColor(ContextCompat.getColor(this, R.color.ep_icon_back));
    }

    public void setCustomToolbarColor(String strCustomColor) {
        if (strCustomColor == null)
            return;
        int customColor = Color.parseColor(strCustomColor);
        // set toolbar bg color
        setToolbarBgColor(customColor);
        // set toolbar text color
        int midColor = getResources().getColor(R.color.custom_middle_color);
        if (customColor >= midColor) {
            // setToolbarTextColor(getResources().getColor(R.color.custom_dark_color));
            setToolbarIconColor(getResources().getColor(R.color.custom_dark_color));
        } else {
            // setToolbarTextColor(getResources().getColor(R.color.custom_light_color));
            setToolbarIconColor(getResources().getColor(R.color.custom_light_color));
        }
        setStatusBarColor(customColor);
        setToolbarIconColor(ContextCompat.getColor(this, R.color.ep_icon_back));
    }

    public void setToolbarBgColor(int color) {
        if (toolbar != null) {
            toolbar.setBackgroundColor(color);
        }
    }

    public void hideToolbar() {
        if (toolbar != null) {
            toolbar.setVisibility(View.GONE);
        }
    }

    public void setToolbarTextColor(int color) {
        if (toolbar != null) {
            // change title text color
            toolbar.setTitleTextColor(color);
            // change toolbar background color
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
            upArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
    }

    public void showAlert(String type, String title, String... desc) {
        if (isActivityFinished(this))
            return;
        final Handler handler = new Handler();
        int[] count = { 0 };

        final Runnable runnable = new Runnable() {
            public void run() {
                if (count[0]++ < 2) {
                    View layout = ConfigToastLayout(type, title, desc);
                    Toast toast = new Toast(getApplicationContext());
                    toast.setGravity(Gravity.FILL_HORIZONTAL, 0, 0);
                    toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.setView(layout);
                    toast.show();
                    handler.postDelayed(this, 3000);
                }
            }
        };
        handler.post(runnable);
    }

    private View ConfigToastLayout(String type, String title, String... desc) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.wmx_alert, (ViewGroup) findViewById(R.id.custom_alert));
        ImageView toast_image = layout.findViewById(R.id.AlertImage);
        TextView toast_tv_titulo = layout.findViewById(R.id.AlertTextTitulo);
        TextView toast_tv_desc = layout.findViewById(R.id.AlertTextDesc);
        LinearLayout toast_ll_custom_alert = layout.findViewById(R.id.custom_alert);
        switch (type) {
            case "success":
                toast_image.setImageResource(R.drawable.check_exito);
                toast_tv_titulo.setTextColor(0xff4AAC38);
                toast_ll_custom_alert.setBackgroundColor(0xffB9E0AB);
                break;
            case "error":
                toast_image.setImageResource(R.drawable.exclamation_mark);
                toast_tv_titulo.setTextColor(Color.parseColor("#FFFFFF"));
                toast_ll_custom_alert.setBackgroundColor(0xffFF9393);
                break;
            case "informative":
                toast_image.setImageResource(R.drawable.efevoo_i_info);
                toast_tv_titulo.setTextColor(Color.parseColor("#FFFFFF"));
                toast_ll_custom_alert.setBackgroundColor(Color.parseColor("#5DADE2"));
                break;
        }
        toast_tv_titulo.setText(title);
        if (desc.length > 0)
            toast_tv_desc.setText(desc[0]);
        return layout;
    }

    public void setInvisiblemargin(boolean status) {
        if (!status) {
            img_invisible_margin.setVisibility(View.GONE);
        }

        else {
            img_invisible_margin.setVisibility(View.VISIBLE);
        }
    }

    public void setWhiteLogo() {
        logo_image.setImageResource(R.drawable.logo_white);
    }

    public void setMarginLogo() {
        container_logo = toolbar.findViewById(R.id.toolbar_logo_container);
        container_logo.setPadding(0, 0, 0, 0);
    }

    public void switch_title_logo(String title) {
        logo_image.setVisibility(View.GONE);
        container_logo.setVisibility(View.GONE);
        txt_toolbar_title.setVisibility(View.VISIBLE);
        setTitle(title);

    }

    protected void hideTicketSpinner() {
        runOnUiThread(() -> {
            if (ticket_progress.isShowing())
                ticket_progress.dismiss();
        });
    }

    public void switch_title_logo(String title, int color) {
        logo_image.setVisibility(View.GONE);
        container_logo.setVisibility(View.GONE);
        txt_toolbar_title.setVisibility(View.VISIBLE);
        setTitle(title);
        txt_toolbar_title.setTextColor(color);
    }

    public String tipofirma(String nip,String entrada){
        int nipParsed = Utils.tryIntParse(nip);
        if(nipParsed == 0) {
            if(entrada.equals("NFC")) return  getString(R.string.wmx_transaction_ticket_contactless_sign);
            if(entrada.equals("ICC")) return  getString(R.string.wmx_transaction_ticket_client_sign);
        }
        if (nipParsed == 1) return getString(R.string.wmx_transaction_ticket_electronic_sign);
       return null;
    }

    public void show_calendar() {
        toolbar_btn_calendar.setVisibility(View.VISIBLE);
    }

    @TargetApi(21)
    public void setStatusBarColor(int color) {
        if (isAboveKITKAT()) {
            Window window = getWindow();
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        if (isAboveKITKAT()) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    public boolean isAboveKITKAT() {
        boolean isHigher = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            isHigher = true;
        }
        return isHigher;
    }

    protected boolean isActivityFinished(Context ctx) {
        return ctx instanceof Activity && ((Activity) ctx).isFinishing();
    }

    /**
     * Use this method to colorize toolbar icons to the desired target color
     *
     * @param toolbarIconsColor the target color of toolbar icons
     */
    public void setToolbarIconColor(int toolbarIconsColor) {
        if (toolbar != null) {
            final PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(toolbarIconsColor,
                    PorterDuff.Mode.SRC_ATOP);// MULTIPLY

            for (int i = 0; i < toolbar.getChildCount(); i++) {
                final View v = toolbar.getChildAt(i);

                // Step 1 : Changing the color of back button (or open drawer button).
                if (v instanceof ImageButton) {
                    // Action Bar back button
                    ((ImageButton) v).getDrawable().setColorFilter(colorFilter);
                }

                if (v instanceof ActionMenuView) {
                    for (int j = 0; j < ((ActionMenuView) v).getChildCount(); j++) {
                        // Step 2: Changing the color of any ActionMenuViews - icons that
                        // are not back button, nor text, nor overflow menu icon.
                        final View innerView = ((ActionMenuView) v).getChildAt(j);

                        if (innerView instanceof ActionMenuItemView) {
                            int drawablesCount = ((ActionMenuItemView) innerView).getCompoundDrawables().length;
                            for (int k = 0; k < drawablesCount; k++) {
                                if (((ActionMenuItemView) innerView).getCompoundDrawables()[k] != null) {
                                    final int finalK = k;

                                    // Important to set the color filter in seperate thread,
                                    // by adding it to the message queue
                                    // Won't work otherwise.
                                    innerView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((ActionMenuItemView) innerView).getCompoundDrawables()[finalK]
                                                    .setColorFilter(colorFilter);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }

                // Step 3: Changing the color of title and subtitle.
                // if(txt_toolbar_title != null) {
                // txt_toolbar_title.setTextColor(toolbarIconsColor);
                // }
                // toolbar.setTitleTextColor(toolbarIconsColor);
                // toolbar.setSubtitleTextColor(toolbarIconsColor);

                // Step 4: Changing the color of the Overflow Menu icon.
                setOverflowButtonColor(this, colorFilter);
            }
        }
    }

    protected void startActivityMiddleware(Intent intent) {
        this.startActivityMiddleware(intent, null);
    }

    @SuppressLint("NewApi")
    protected int getDisplayState() {
        DisplayManager dm = (DisplayManager) this.getSystemService(Context.DISPLAY_SERVICE);
        return dm.getDisplay(0).getState();
    }

    private void NotNetworkDialog() {
        NotConnectionDialog dialog = new NotConnectionDialog(this);
        FragmentManager manager = getSupportFragmentManager();
        if(manager.executePendingTransactions()) return;
        dialog.show(getSupportFragmentManager(), null);
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork == null) return false;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    protected void setThemeColor(int color) {
        try {
            if (actionbar != null) {
                actionbar.setBackgroundColor(color);
            }
            StatusBarCompat.compat(this, color);
        } catch (Exception e) {

        }
    }
    public boolean resolveNetworkFlag(HashMap<FLAGS, Object> Flags) {
        if(Flags == null) return true;
        boolean networkFlag = (boolean) Utils.isNull(Flags.get(FLAGS.CHECK_NETWORK), false);
        if(networkFlag && !isNetworkAvailable()) {
            NotNetworkDialog();
            return false;
        }
        return true;
    }

    public HashMap<FLAGS, Object> getFlags(String key) {
        return ActivityFlags.getInstance().getByKey(key);
    }

    private boolean resolveFlags(Intent intent) {
        HashMap<FLAGS, Object> Flags = getFlags(intent.getComponent().getClassName());
        //Resolve flags
        if(!resolveNetworkFlag(Flags)) return false;
        return true;
    }

    protected void startActivityMiddleware(Intent intent, @Nullable Bundle options) {
        if(!resolveFlags(intent)) return;
        String CurrPackageName = getPackageName();
        ComponentName name = intent.resolveActivity(getPackageManager());
        String intentPackageName = name.getPackageName();
        String intentClassName = name.getClassName();
        if (intentPackageName.equals(CurrPackageName) && intentClassName.contains(CurrPackageName)) {
            startActivity(intent, options);
        }
    }

    private void setOverflowButtonColor(final Activity activity, final PorterDuffColorFilter colorFilter) {
        final String overflowDescription = activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final ArrayList<View> outViews = new ArrayList<View>();
                decorView.findViewsWithText(outViews, overflowDescription,
                        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
                if (outViews.isEmpty()) {
                    return;
                }
                AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
                overflow.setColorFilter(colorFilter);
                removeOnGlobalLayoutListener(decorView, this);
            }
        });
    }

    private void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            v.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        } else {
            v.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    class MyPrinterListener implements PrintListener {

        @Override
        public void printResult(boolean b, String status, int type) {
            TRACE.d("printResult: " + status);
            hideTicketSpinner();
            ticketHandler.removeCallbacksAndMessages(null);
            if (b) {
                onPrintFinished(true, entity_print, ticketLayoutType);
            } else {
                onPrintError(false, status, entity_print, ticketLayoutType);
            }
        }
    }

}
