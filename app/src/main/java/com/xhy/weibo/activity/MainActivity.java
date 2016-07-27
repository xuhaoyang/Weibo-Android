package com.xhy.weibo.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.xhy.weibo.AppConfig;
import com.xhy.weibo.IMessageListener;
import com.xhy.weibo.IMessageServiceRemoteBinder;
import com.xhy.weibo.R;
import com.xhy.weibo.adapter.StatusAdpater;
import com.xhy.weibo.base.BaseActivity;
import com.xhy.weibo.constants.CommonConstants;
import com.xhy.weibo.db.DBManager;
import com.xhy.weibo.db.UserDB;
import com.xhy.weibo.logic.StatusLogic;
import com.xhy.weibo.logic.UserLoginLogic;
import com.xhy.weibo.model.StatusGroup;
import com.xhy.weibo.model.Status;
import com.xhy.weibo.model.User;
import com.xhy.weibo.network.URLs;
import com.xhy.weibo.service.MessageService;
import com.xhy.weibo.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, StatusLogic.GetStatusGroupCallBack, StatusLogic.GetStatusListCallBack, UserLoginLogic.GetUserinfoCallBack {

    public static final int REQUEST_CODE_WRITE_FORWARD = 2;
    public static final int REQUEST_CODE_WRITE_STATUS = 3;

    public static final int GROUP = 0;
    public static final int ALL_ITEMID = 0;

    @BindView(R.id.recycler_view_home)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.main_Car)
    CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.swipeRefreshLayout_home)
    SwipeRefreshLayout mSwipeRefreshLayout;

    Button btnHot;
    Button btnKeep;
    Button btnSettings;
    ImageView headerView_iv_avatar;
    TextView headerView_tv_username;

    SearchView actionView;
    View headerView;
    LinearLayoutManager linearLayoutManager;

    List<Status> statuses = new ArrayList<Status>();
    private ActionBarDrawerToggle toggle;
    private String gid = "";
    private StatusAdpater statusAdpater = new StatusAdpater(statuses, this);
    private boolean isLoading;
    private Handler handler = new Handler();
    private int currPage = 1;


    private int totalPage = 1;
    private int lastVisibleItemPosition;
    private SearchView.SearchAutoComplete mEditSearch;
    private DBManager dbManager;
    private SQLiteDatabase db;
    private UserDB userDB;
    private IMessageServiceRemoteBinder binder;
    private Menu menu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLog("onCreate");
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initUserinfo();

    }

    private void init() {
        initView();
        initNavigationMenu();
        initListener();
        initRecyclerView();
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
                LoadData();
            }
        });

        //启动推送
        Intent intent = new Intent(this, MessageService.class);
//        intent.putExtra("ACCOUNT", AppConfig.getAccount());
//        intent.putExtra("PASSWORD", AppConfig.getPassword());
//        intent.putExtra("USERID", AppConfig.getUserId());
        intent.putExtra("TOKEN", AppConfig.ACCESS_TOKEN.getToken());

        startService(intent);
    }

    private void initView() {
        setSupportActionBar(toolbar);

        headerView = navigationView.getHeaderView(0);
        btnHot = (Button) headerView.findViewById(R.id.btnHot);
        btnKeep = (Button) headerView.findViewById(R.id.btnKeep);
        btnSettings = (Button) headerView.findViewById(R.id.btnSettings);
        headerView_iv_avatar = (ImageView) headerView.findViewById(R.id.iv_avatar);
        headerView_tv_username = (TextView) headerView.findViewById(R.id.username);

        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
    }

    private void initUserinfo() {
        initDB();
        List<User> users = userDB.QueryUsers(db, "id=?", new String[]{AppConfig.getUserId() + ""});
        dbManager.closeDatabase();
        if (!users.isEmpty()) {
            long time = System.currentTimeMillis() - users.get(0).getUptime();
            //大于半天刷新下缓存的用户数据
            if (time > 43200000 || TextUtils.isEmpty(users.get(0).getUsername())) {
                UpdataUserinfo();
            } else {
                String url = URLs.AVATAR_IMG_URL + users.get(0).getFace();
                ImageUtils.setImage(headerView_iv_avatar, url);
                headerView_tv_username.setText(users.get(0).getUsername());
            }
        } else {
            UpdataUserinfo();
        }
    }

    private void UpdataUserinfo() {

        UserLoginLogic.getUserinfo(this, AppConfig.getUserId(), null, 0, AppConfig.ACCESS_TOKEN.getToken(), this);

    }

    @Override
    public void onUserInfoSuccess(User user) {
        initDB();
        List<User> users = userDB.QueryUsers(db, "id=?", new String[]{user.getUid() + ""});
        if (!users.isEmpty()) {
            userDB.updateUser(db, user);
        } else {
            userDB.insertUser(db, user);
        }
        String url = URLs.AVATAR_IMG_URL + user.getFace();
        ImageUtils.setImage(headerView_iv_avatar, url);
        headerView_tv_username.setText(user.getUsername());
        dbManager.closeDatabase();
    }

    @Override
    public void onUserInfoFailure(int errorCode, String errorMessage) {
        showSnackbar("错误：" + errorCode + "," + errorMessage);
    }

    @Override
    public void onUserInfoError(Throwable error) {
        showSnackbar("获取用户信息失败");
    }


    private void initDB() {
        //获取数据
        dbManager = new DBManager(this);
        dbManager.openDatabase();
        db = dbManager.getDatabase();
        userDB = new UserDB(this);
    }

    /**
     * 记得做缓存
     */
    private void initNavigationMenu() {
        menu = navigationView.getMenu();
        //參數1:群組id, 參數2:itemId, 參數3:item順序, 參數4:item名稱
        menu.add(GROUP, ALL_ITEMID, ALL_ITEMID, "全部");


        StatusLogic.getStatusGroup(this, AppConfig.getUserId(), AppConfig.ACCESS_TOKEN.getToken(), this);

    }

    @Override
    public void onGroupSuccess(List<StatusGroup> statusGroups) {
        for (StatusGroup sg : statusGroups) {
            menu.add(GROUP, sg.getId(), sg.getId(), sg.getName());
        }
    }

    @Override
    public void onGroupFailure(String message) {
        showSnackbar(message);
        showLog(message);
    }

    @Override
    public void onGroupError(Throwable t) {
        showLog(t.getMessage());
    }

    private void LoadData() {

        StatusLogic.getStatusList(this, AppConfig.getUserId(), currPage, AppConfig.ACCESS_TOKEN.getToken(), gid, 0, this);
    }

    @Override
    public void onStatusListSuccess(List<Status> statuses, int totalPage) {
        this.totalPage = totalPage;
        if (currPage == 1) {
            this.statuses.clear();
            this.statuses.addAll(statuses);
            statusAdpater.setLastAnimatedPosition(5);
        } else {
            //要判断是否有重复的
            for (Status s : statuses) {
                if (!this.statuses.contains(s)) {
                    this.statuses.add(s);
                }
            }
        }
        statusAdpater.notifyDataSetChanged();
        stopRefresh();
    }

    @Override
    public void onStatusListFailure(String message) {
        showSnackbar(message);
        stopRefresh();
    }

    @Override
    public void onStatusListError(Throwable t) {
        showLog(t.getMessage());
        isLoading = false;
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void stopRefresh() {
        mSwipeRefreshLayout.setRefreshing(false);
        if (statusAdpater != null) {
            statusAdpater.notifyItemRemoved(statusAdpater.getItemCount());
        }
        isLoading = false;
    }


    private void initListener() {
        navigationView.setNavigationItemSelectedListener(this);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent(MainActivity.this, WriteStatusActivity.class);
                data.putExtra(WriteStatusActivity.TYPE, WriteStatusActivity.NEW_STATUS_TYPE);
                startActivityForResult(data, REQUEST_CODE_WRITE_STATUS);
            }
        });

        btnHot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HotsActivity.class));
            }
        });
        btnKeep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, KeepStatusActivity.class));
            }
        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currPage = 1;
                LoadData();
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
//                showLog("StateChanged = " + newState);
                int pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition();
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && lastVisibleItemPosition + 1 == statusAdpater.getItemCount() && pastVisiblesItems != 0) {
                    //这里的判断条件还会导致有点BUG,假设条数不足5条,上拉是无法刷新的,只能通过下拉
                    if (!isLoading) {
                        isLoading = true;
                        if (currPage <= totalPage && statuses.size() > 0) {
                            currPage += 1;
                        }
                        if (!mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(true);
                        }
                        LoadData();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();

            }
        });

        headerView_iv_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent(MainActivity.this, UserInfoActivity.class);
                data.putExtra(UserInfoActivity.USER_ID, AppConfig.getUserId());
                startActivity(data);
            }
        });
    }


    public void initRecyclerView() {
        linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(statusAdpater);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_WRITE_FORWARD:
                mSwipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(true);
                        currPage = 1;
                        LoadData();
                        linearLayoutManager.scrollToPosition(0);
                    }
                });
                break;
            case REQUEST_CODE_WRITE_STATUS:
                boolean sendSuccess = data.getBooleanExtra(WriteStatusActivity.SEND_STATUS_SUCCESS, false);
                if (sendSuccess) {
                    Snackbar.make(mCoordinatorLayout, "发送成功", Snackbar.LENGTH_LONG)
                            .show();
                    mSwipeRefreshLayout.setRefreshing(true);
                    currPage = 1;
                    LoadData();
                    linearLayoutManager.scrollToPosition(0);
                } else {
                    Snackbar.make(mCoordinatorLayout, "发送失败", Snackbar.LENGTH_LONG)
                            .show();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        final MenuItem item = menu.findItem(R.id.action_search);
        actionView = (SearchView) MenuItemCompat.getActionView(item);
        mEditSearch = (SearchView.SearchAutoComplete) actionView.findViewById(R.id.search_src_text);
        actionView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (TextUtils.isEmpty(query)) {
                    return false;
                }
                Intent data = new Intent(MainActivity.this, SearchActivity.class);
                data.putExtra(SearchActivity.SEARCH_CONTENT, query);
                startActivity(data);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
        switch (id) {
            case R.id.action_notifications:
                intent2Activity(NotifyActivity.class);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        currPage = 1;
        int id = item.getItemId();
        showLog("onNavigationItemSelected:" + id);
        switch (id) {
            case ALL_ITEMID:
                gid = "";
                mSwipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(true);
                        LoadData();
                    }
                });
                break;
            default:
                gid = id + "";
                statuses.clear();
                statusAdpater.notifyDataSetChanged();
                mSwipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(true);
                        LoadData();
                    }
                });
                break;
        }


        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
//        binder = IMessageServiceRemoteBinder.Stub.asInterface(service);
//        try {
//            binder.setMessageListener(new IMessageListener.Stub() {
//                @Override
//                public void setAccessToken(String token, long time) throws RemoteException {
//                    AppConfig.ACCESS_TOKEN.setToken(token);
//                    AppConfig.ACCESS_TOKEN.setTokenStartTime(time);
//                    showLog("-->更新TOKEN成功");
//                }
//
//            });
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//        showLog("Service Connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        showLog("Service disConnected");
    }

    @Override
    protected void onResume() {
        super.onResume();
//        bindService(new Intent(this, MessageService.class), this, Context.BIND_AUTO_CREATE);
        showLog("onResume");

    }

    @Override
    protected void onStop() {
        super.onStop();
        showLog("onStop");

    }


    private void showSnackbar(String msg) {
        showSnackbar(msg, Snackbar.LENGTH_SHORT);
    }

    private void showSnackbar(String msg, int length) {
        Snackbar.make(mCoordinatorLayout, msg, length).show();
    }


}
