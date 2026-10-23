package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivityVodBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.SiteListener;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.ui.fragment.FolderFragment;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Optional;

public class VodActivity extends BaseActivity implements TypeAdapter.OnClickListener, SiteListener {

    private ActivityVodBinding mBinding;
    private SiteViewModel mViewModel;
    private TypeAdapter mAdapter;
    private Site mPendingSite;
    private Result mResult;
    private String mKey;
    private View mOldView;
    private boolean mLoading;

    public static void start(Activity activity, Result result) {
        start(activity, VodConfig.get().getHome().getKey(), result);
    }

    public static void start(Activity activity, String key, Result result) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, VodActivity.class);
        intent.putExtra("key", key);
        intent.putExtra("result", result);
        activity.startActivity(intent);
    }

    private String getKey() {
        return mKey != null ? mKey : getIntent().getStringExtra("key");
    }

    private Result getResult() {
        return mResult != null ? mResult : getIntent().getParcelableExtra("result");
    }

    private Class getType() {
        return mAdapter.get(mBinding.pager.getCurrentItem());
    }

    private FolderFragment getFragment() {
        return (FolderFragment) mBinding.pager.getAdapter().instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVodBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mKey = getIntent().getStringExtra("key");
        mResult = getIntent().getParcelableExtra("result");
        setRecyclerView();
        setTypes();
        setPager();
        bindSite();
        setViewModel();
    }

    @Override
    protected void initEvent() {
        mBinding.site.setOnClickListener(v -> showSiteDialog());
        mBinding.site.setOnLongClickListener(v -> {
            showSiteDialog();
            return true;
        });
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.recycler.setSelectedPosition(position);
                if (!mBinding.site.hasFocus()) mBinding.recycler.requestFocus();
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onChildSelected(child);
            }
        });
    }

    private void setRecyclerView() {
        if (!getIntent().getBooleanExtra("focusSite", false)) mBinding.recycler.requestFocus();
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.recycler.setAdapter(mAdapter = new TypeAdapter(this));
    }

    private void setTypes() {
        mAdapter.addAll(getResult().getTypes());
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.getResult().observe(this, this::onHomeResult);
    }

    private void bindSite() {
        Site site = VodConfig.get().getSite(getKey());
        if (site.isEmpty()) site = VodConfig.get().getHome();
        mBinding.site.setText(getString(R.string.vod_site, siteName(site)));
        if (getIntent().getBooleanExtra("focusSite", false)) mBinding.site.requestFocus();
    }

    private void showSiteDialog() {
        if (mLoading) return;
        SiteDialog.create().show(this);
    }

    private void switchSite(boolean next) {
        List<Site> sites = VodConfig.get().getSites().stream().filter(site -> !site.isHide()).toList();
        if (sites.size() < 2) return;
        int index = 0;
        for (int i = 0; i < sites.size(); i++) if (sites.get(i).getKey().equals(getKey())) index = i;
        index = next ? (index + 1) % sites.size() : (index - 1 + sites.size()) % sites.size();
        setSite(sites.get(index));
    }

    private void setLoading(boolean loading) {
        mLoading = loading;
        mBinding.site.setEnabled(!loading);
        mBinding.loading.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void onHomeResult(Result result) {
        if (!mLoading || isFinishing()) return;
        Site site = mPendingSite;
        mPendingSite = null;
        setLoading(false);
        if (site == null) return;
        if (result == null || result.getTypes().isEmpty()) {
            bindSite();
            if (result == null || !result.hasMsg()) Notify.show(R.string.error_empty);
            return;
        }
        Cache.clear().put(result);
        VodConfig.get().setHome(site);
        Intent intent = new Intent(this, VodActivity.class);
        intent.putExtra("key", site.getKey());
        intent.putExtra("result", result);
        intent.putExtra("focusSite", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    private static String siteName(Site site) {
        return site.getName().isEmpty() ? site.getKey() : site.getName();
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child) {
        if (mOldView != null) mOldView.setSelected(false);
        if ((mOldView = child != null ? child.itemView : null) == null) return;
        mOldView.setSelected(true);
        App.post(mRunnable, 100);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mBinding.pager.setCurrentItem(mBinding.recycler.getSelectedPosition());
        }
    };

    private boolean isFilterVisible() {
        return Optional.ofNullable(getType()).map(Class::getFilter).orElse(false);
    }

    private void updateFilter() {
        Optional.ofNullable(getType()).ifPresent(this::updateFilter);
    }

    private void updateFilter(Class item) {
        item.setFilter(!item.getFilter());
        getFragment().toggleFilter(item.getFilter());
        mAdapter.notifyItemRangeChanged(mAdapter.indexOf(item), 1);
    }

    public void closeFilter() {
        if (isFilterVisible()) updateFilter();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.CATEGORY) getFragment().onRefresh();
    }

    @Override
    public void onItemClick(Class item) {
        updateFilter(item);
    }

    @Override
    public void onRefresh(Class item) {
        getFragment().onRefresh();
    }

    @Override
    public void setSite(Site item) {
        if (item == null || item.isEmpty() || mLoading || item.getKey().equals(getKey())) return;
        mPendingSite = item;
        mBinding.site.setText(getString(R.string.vod_site, siteName(item)));
        setLoading(true);
        Notify.show(getString(R.string.play_switch_site, siteName(item)));
        mViewModel.homeContent(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mBinding.site.hasFocus() && KeyUtil.isActionDown(event) && event.getRepeatCount() == 0 && !mLoading) {
            if (KeyUtil.isLeftKey(event)) {
                switchSite(false);
                return true;
            }
            if (KeyUtil.isRightKey(event)) {
                switchSite(true);
                return true;
            }
        }
        if (KeyUtil.isMenuKey(event)) updateFilter();
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onBackInvoked() {
        if (isFilterVisible()) updateFilter();
        else if (getFragment().moveToTop()) return;
        else if (getFragment().canBack()) getFragment().goBack();
        else super.onBackInvoked();
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            return FolderFragment.newInstance(getKey(), type);
        }

        @Override
        public int getCount() {
            return mAdapter.getItemCount();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
