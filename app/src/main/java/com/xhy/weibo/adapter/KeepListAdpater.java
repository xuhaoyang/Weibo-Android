package com.xhy.weibo.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.xhy.weibo.R;
import com.xhy.weibo.model.Status;
import com.xhy.weibo.api.URLs;
import com.xhy.weibo.ui.activity.UserInfoActivity;
import com.xhy.weibo.ui.activity.WriteStatusActivity;
import com.xhy.weibo.utils.Constants;
import com.xhy.weibo.utils.DateUtils;
import com.xhy.weibo.utils.StringUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by xuhaoyang on 16/5/16.
 */
public class KeepListAdpater extends RecyclerView.Adapter {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;
    private Context mContext;
    private List<Status> mStatuses;

    public KeepListAdpater(Context mContext, List<Status> mStatuses) {
        this.mContext = mContext;
        this.mStatuses = mStatuses;
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_avatar)
        public ImageView iv_avatar;
        @BindView(R.id.tv_subhead)
        public TextView tv_subhead;
        @BindView(R.id.tv_content)
        public TextView tv_content;
        @BindView(R.id.tv_caption)
        public TextView tv_caption;
        @BindView(R.id.cv_item)
        public CardView cv_item;

        public ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class FootViewHolder extends RecyclerView.ViewHolder {

        public FootViewHolder(View view) {
            super(view);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            ItemViewHolder root = new ItemViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false));
            return root;
        } else if (viewType == TYPE_FOOTER) {
            FootViewHolder root = new FootViewHolder(
                    LayoutInflater.from(parent.getContext()).
                            inflate(R.layout.item_comment_footer_end, parent, false));
            return root;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            final ItemViewHolder viewHolder = (ItemViewHolder) holder;
            final Status status = mStatuses.get(position);


            //设置头像
            if (TextUtils.isEmpty(status.getUserinfo().getFace50())) {
                viewHolder.iv_avatar.setImageResource(R.mipmap.ic_launcher);
            } else {
                String url = URLs.AVATAR_IMG_URL + status.getUserinfo().getFace50();
                Glide.with(viewHolder.iv_avatar.getContext()).load(url).
                        fitCenter().into(viewHolder.iv_avatar);
            }

            viewHolder.iv_avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent data = new Intent(mContext, UserInfoActivity.class);
                    data.putExtra(Constants.USER_ID, status.getUid());
                    mContext.startActivity(data);
                }
            });

            //设置用户名
            viewHolder.tv_subhead.setText(status.getUserinfo().getUsername());
            //设置时间
            viewHolder.tv_caption.setText(DateUtils.getShotTime(status.getTime()));
            //设置正文
            viewHolder.tv_content.setText(
                    StringUtils.getWeiboContent(mContext,
                            viewHolder.tv_content, status.getContent()));

            viewHolder.cv_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopupMenu(viewHolder.tv_caption, status);

                }
            });
        }

    }

    public void showPopupMenu(View view, final Status status) {
        //参数View 是设置当前菜单显示的相对于View组件位置，具体位置系统会处理
        PopupMenu popupMenu = new PopupMenu(mContext, view);
        //加载menu布局
        popupMenu.getMenuInflater().inflate(R.menu.menu_status_detail_forward, popupMenu.getMenu());
        //设置menu中的item点击事件
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.action_commet_forward:
                        Intent data = new Intent(mContext, WriteStatusActivity.class);
                        //评论方式
                        data.putExtra(Constants.TYPE, Constants.FORWARD_TYPE);
                        data.putExtra(Constants.TAG, Constants.COMMENT_ADPATER_CODE);
                        data.putExtra(Constants.STATUS_INTENT, status);
                        mContext.startActivity(data);
                        break;
//                    case R.id.action_commet_forward:
//                        break;
                }
                return true;
            }
        });
        //设置popupWindow消失的点击事件
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {

            }
        });

        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return mStatuses.size() + 1;
        //mComments.size() == 0 ? 0 :
    }

    @Override
    public int getItemViewType(int position) {
        if (position + 1 == getItemCount()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_ITEM;
        }
    }

}
