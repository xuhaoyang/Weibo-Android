package com.xhy.weibo.ui.activity.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.xhy.weibo.AppConfig;
import com.xhy.weibo.R;
import com.xhy.weibo.model.DialogData;
import com.xhy.weibo.model.Item;
import com.xhy.weibo.model.Setting;
import com.xhy.weibo.ui.base.ListFragment;
import com.xhy.weibo.ui.vh.SettingHeadViewHolder;
import com.xhy.weibo.ui.vh.SettingItemViewHolder;
import com.xhy.weibo.utils.RecycleViewDivider;

import java.util.ArrayList;
import java.util.List;

import hk.xhy.android.common.ui.vh.OnListItemClickListener;
import hk.xhy.android.common.ui.vh.ViewHolder;
import hk.xhy.android.common.utils.ConstUtils;
import hk.xhy.android.common.utils.ConvertUtils;
import hk.xhy.android.common.utils.ErrorUtils;
import hk.xhy.android.common.utils.TimeUtils;
import hk.xhy.android.common.utils.ViewUtils;
import hk.xhy.android.common.widget.PullToRefreshMode;

/**
 * Created by xuhaoyang on 2017/3/9.
 */

public class SettingNotificationFragment extends ListFragment<ViewHolder, Setting, List<Setting>, FrameLayout> implements OnListItemClickListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public static SettingNotificationFragment newInstance() {
        return new SettingNotificationFragment();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //设置item间间隔样式
        getRecyclerView().addItemDecoration(new RecycleViewDivider(getmActivity(),
                LinearLayoutManager.VERTICAL));
        //设置下拉刷新颜色
        getPullToRefreshLayout().setColorSchemeResources(R.color.colorPrimary);
        /* 解决刷新动画出不来的问题 */
        getPullToRefreshLayout().setProgressViewOffset(false, 0, (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources()
                        .getDisplayMetrics()));
        setMode(PullToRefreshMode.DISABLED);

        setFooterShowEnable(false);
        initLoader();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting_list, container, false);
        return view;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = null;
        switch (viewType) {
            case Setting.ITEM_HEAD:
                holder = new SettingHeadViewHolder(ViewUtils.inflate(parent, R.layout.item_head_title));
                break;
            case Setting.ITEM_SINGLE:
                holder = new SettingItemViewHolder(ViewUtils.inflate(parent, R.layout.item_single_config));
                break;
            case Setting.ITEM_TWICE:
                holder = new SettingItemViewHolder(ViewUtils.inflate(parent, R.layout.item_twice_config));

                break;
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (holder instanceof SettingItemViewHolder) {
            //判断是否隐藏item分隔线
            boolean isHide = false;
            if (position + 1 <= getItemCount() - 1) {
                if (getItemsSource().get(position + 1).getConfig() == Setting.ITEM_HEAD) {
                    isHide = true;
                }
            } else {
                isHide = true;
            }
            ((SettingItemViewHolder) holder).bind(getItemsSource().get(position), this, isHide);
        } else if (holder instanceof SettingHeadViewHolder) {
            ((SettingHeadViewHolder) holder).bind(getmActivity(), getItemsSource().get(position));
        }

    }

    @Override
    public List<Setting> onLoadInBackground() throws Exception {
        List<Setting> settings = new ArrayList<>();

        Setting head = new Setting();
        head.setId(0);
        head.setWeight(0);
        head.setConfig(Setting.ITEM_HEAD);
        head.setMainHead(getString(R.string.title_item_regular_settings));
        settings.add(head);

        Setting notify = new Setting();
        notify.setId(1);
        notify.setWeight(1);
        notify.setConfig(Setting.ITEM_SINGLE);
        notify.setCheckBoxIs(AppConfig.isNotify());
        notify.setMainHead(getString(R.string.title_item_notifications));
        settings.add(notify);

        Setting notify2 = new Setting();
        notify2.setId(2);
        notify2.setWeight(2);
        notify2.setConfig(Setting.ITEM_TWICE);
        notify2.setCheckBoxIs(AppConfig.getDoNotDisturb());
        notify2.setMainHead(getString(R.string.title_item_donotdisturb_mode));
        notify2.setSubHead(getString(R.string.title_item_donotdisturb_mode_content));
        settings.add(notify2);


        final Setting interval = new Setting();
        DialogData<Integer> intervalData = new DialogData();
        intervalData.setConfig(DialogData.RAIDO);
        intervalData.setId(0);
        final String[] intervalItem = getResources().getStringArray(R.array.dialog_content_item_notification_interval);
        final int[] intervalItemValue = getResources().getIntArray(R.array.dialog_content_item_notification_interval_values);
        intervalData.setItems(new ArrayList<Item<Integer>>() {{
            for (int i = 0; i < intervalItem.length; i++) {
                add(new Item(0, intervalItem[i], intervalItemValue[i]));
            }
        }});
        interval.setId(3);
        interval.setWeight(3);
        interval.setConfig(Setting.ITEM_TWICE);
        interval.setFunctionConfig(Setting.FUNCTION_ITEM_DIALOG);
        interval.setMainHead(getString(R.string.title_item_notification_interval));
        interval.setSubHead(TimeUtils.milliseconds2Unit(AppConfig.getNotificaitonInterval(), ConstUtils.MIN) + "分钟");
        interval.setDialogData(intervalData);

        settings.add(interval);

        return settings;

    }

    @Override
    public void onLoadComplete(List<Setting> data) {
        getItemsSource().clear();
        getItemsSource().addAll(data);
        getAdapter().notifyDataSetChanged();
        onRefreshComplete();
    }

    @Override
    public void onLoadError(Exception e) {
        super.onLoadError(e);
        ErrorUtils.show(getmActivity(), e);
    }

    @Override
    public int getItemViewType(int position) {
        Setting setting = getItemsSource().get(position);
        return setting.getConfig();
    }

    @Override
    public void OnListItemClick(int postion) {
        final Setting setting = getItemsSource().get(postion);
        final int id = setting.getId();
        switch (id) {
            case 1:
                if (AppConfig.isNotify()) {
                    AppConfig.setNotify(false);
                } else {
                    AppConfig.setNotify(true);
                }
                break;
            case 2:
                if (AppConfig.getDoNotDisturb()) {
                    AppConfig.setDoNotDisturb(false);
                } else {
                    AppConfig.setDoNotDisturb(true);
                }

                break;
            case 3:
                showDialog(getmActivity(), setting, new SaveDatas<Integer>() {
                    @Override
                    public void save(Integer value) {
                        AppConfig.setNotificationInterval(value);
                        restartLoader();//刷新界面
                    }
                });
                break;
        }
        switch (setting.getFunctionConfig()) {
            case Setting.FUNCTION_ITEM_OPTIONS:
                break;
            case Setting.FUNCTION_ITEM_DIALOG:

                break;
        }

        restartLoader();
    }

    public void showDialog(final Context context, final Setting setting, final SaveDatas callBack) {

        switch (setting.getDialogData().getConfig()) {
            case DialogData.RAIDO:
                LinearLayout linearLayout = new LinearLayout(context);
                final int layout_px_16 = ConvertUtils.dp2px(16);
                final RadioGroup radioGroup = new RadioGroup(context);

                linearLayout.setLayoutParams(
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.setPadding(layout_px_16, layout_px_16, layout_px_16, layout_px_16);

                radioGroup.setOrientation(RadioGroup.VERTICAL);
                radioGroup.setLayoutParams(new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                final ArrayList<Item> items = setting.getDialogData().getItems();
                for (int i = 0; i < items.size(); i++) {
                    final Item item = items.get(i);
                    final RadioButton rb = new RadioButton(context);
                    rb.setText(item.getName());
                    final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(layout_px_16, 0, 0, layout_px_16);
                    rb.setLayoutParams(layoutParams);
                    rb.setTextSize(16f);
                    radioGroup.addView(rb);
                }

                linearLayout.addView(radioGroup);


                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(setting.getMainHead());
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        for (int i = 0; i < radioGroup.getChildCount(); i++) {
                            RadioButton rd = (RadioButton) radioGroup.getChildAt(i);
                            if (rd.isChecked()) {
                                for (int j = 0; j < items.size(); j++) {
                                    final Item item = items.get(j);
                                    final String name = item.getName();
                                    if (name != null && name.equals(rd.getText().toString())) {
                                        callBack.save(item.getValue());
                                    }
                                }
                            }
                        }

                        getAdapter().notifyDataSetChanged();


                    }
                });
                builder.setCancelable(true);
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setView(linearLayout);
                builder.show();
                break;
        }

    }

    @Override
    public void OnItemOtherClick(int postion, int type) {

    }

    public interface SaveDatas<T extends Object> {
        void save(T value);
    }

}