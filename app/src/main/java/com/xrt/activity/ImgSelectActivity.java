package com.xrt.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.xrt.R;
import com.xrt.authority.AuthorityController;
import com.xrt.authority.AuthorityCtrlFactory;
import com.xrt.accesser.systempic.SystemPicAccesser;
import com.xrt.constant.CommonExtraTag;
import com.xrt.tools.UiTools;
import com.xrt.widget.HorizonImageTextButtonGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ImgSelectActivity extends AppCompatActivity {
    private GridLayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private SystemPicAccesser mSystemPicAccesser;
    private List<Integer> mSelectedPosList = new ArrayList<>();
    private DisplayMetrics mDisplayMetrics;
    private int createCounter;
    //view fields
    private ConstraintLayout mRootView;
    private RecyclerView mRecyclerView;
    private Button mBackButton;
    private Button mCompleteButton;
    private ProgressBar mProgressbarLoadingImg;
    private View mPreviewPopWindowRootview;
    private PopupWindow mPreviewPopWindow;
    private ImageView mPreviewImageViewInPopWindow;
    private TextView mMask;
    private HorizonImageTextButtonGroup mFootbarButtonGroup;
    private HorizonImageTextButtonGroup.ImageTextButton mClearSelectButton;
    private AuthorityController mAuthorityCtr;
    //常量
    private static final int EACH_ROW_COUNT = 4;
    //局部刷新相关变量
    private static final int UPDATE_CHECK_BUTTON_ORDER = 1;
    private static final int CLEAR_SELECT = 2;
    //权限相关
    private static final int REQUEST_WRITE_AUTHORITY = 0x10;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgselect);
        initNormalVar();
        initView();
        initFootbarButtonGroup();
        setViewsListener();
        initRecyclerView();
        initSystemPicAccesser();
        validAuthorityAndLoadSystemPics();
    }
    private void validAuthorityAndLoadSystemPics(){
        boolean hasAuthority = mAuthorityCtr.validPermissionsWithHint(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_AUTHORITY);
        if (hasAuthority){
            loadSystemPics();
        }
    }
    private void loadSystemPics(){
        Thread thread = new Thread(() -> {
            mSystemPicAccesser.init();
        });
        thread.start();
    }
    private void initRecyclerView(){
        mLayoutManager = new GridLayoutManager(this, 4);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new RecyclerAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }
    private void initSystemPicAccesser(){
        mSystemPicAccesser = new SystemPicAccesser(getContentResolver());
        mSystemPicAccesser.setImgsLoadedListener(() -> {
            mRecyclerView.post(() -> {
                mAdapter.notifyDataSetChanged();//RV在完成初始化后就会回调getItemCount开始进行Item的布局。但是RV初始化完图片的URI可能还没加载完，所以URI加载完之后要执行一次RV的数据更新通知。
                mProgressbarLoadingImg.setVisibility(View.INVISIBLE);
            });
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_AUTHORITY && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            loadSystemPics();
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder{
        ConstraintLayout rootView;
        ImageView imageView;
        Button checkButton;

        public ItemViewHolder(View view){
            super(view);
            rootView = view.findViewById(R.id.rootview_item_recyclerview_activity_imgselect);
            imageView = view.findViewById(R.id.imageview_item_recyclerview_activity_imgselect);
            checkButton = view.findViewById(R.id.button_item_check_activity_imgselect);
        }
    }
    public class RecyclerAdapter extends RecyclerView.Adapter<ItemViewHolder>{
        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup viewParent, int viewType){
            View view = getLayoutInflater().inflate(R.layout.item_recyclerview_activity_imgselect, viewParent, false);
            int eachViewHeight = viewParent.getWidth() / EACH_ROW_COUNT;
            view.getLayoutParams().height = eachViewHeight;
            //Log.d("mxrt", "create:" + ++createCounter);
            return new ItemViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position){
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position, List<Object> payLoads){
            if (payLoads.isEmpty()){
                Uri imgUri = mSystemPicAccesser.getImgUriByPosition(position);
                Glide.with(ImgSelectActivity.this).load(imgUri).centerCrop().into(viewHolder.imageView);
                setItemCheckButtonClickListener(viewHolder.checkButton, position);
                if (mSelectedPosList.contains(position)){
                    viewHolder.checkButton.setText("" + (mSelectedPosList.indexOf(position) + 1));
                    viewHolder.checkButton.setBackgroundResource(R.drawable.button_item_check_activity_imgselect_press_bg);
                }else{
                    viewHolder.checkButton.setText("");
                    viewHolder.checkButton.setBackgroundResource(R.drawable.button_item_check_activity_imgselect_unpress_bg);
                }
            }else{
                for (int i = 0; i < payLoads.size(); i++){
                    int flag = (int)payLoads.get(i);
                    switch (flag){
                        case UPDATE_CHECK_BUTTON_ORDER:
                            if (mSelectedPosList.contains(position)){
                                viewHolder.checkButton.setText("" + (mSelectedPosList.indexOf(position) + 1));
                            }
                            break;
                        case CLEAR_SELECT:
                            viewHolder.checkButton.setText("");
                            viewHolder.checkButton.setBackgroundResource(R.drawable.button_item_check_activity_imgselect_unpress_bg);
                            break;
                    }
                }
            }
            setItemClickListener(viewHolder.rootView, position);
        }
        @Override
        public int getItemCount(){
            //Log.d("mxrt", "getcount");
            return mSystemPicAccesser.getAllImgCount();
        }
    }
    public class ItemDecoration extends RecyclerView.ItemDecoration{
        @Override
        public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state){

        }
    }
    private void initNormalVar(){
        mDisplayMetrics = UiTools.getScreenMetrics(this);
        mAuthorityCtr = AuthorityCtrlFactory.createAuthorityCtrl(this);
    }
    private void initView(){
        mRootView = findViewById(R.id.rootview_activity_imgselect);
        mRecyclerView = findViewById(R.id.recyclerview_activity_imgselect);
        mBackButton = findViewById(R.id.button_quit_toolbar_activity_imgselect);
        mCompleteButton = findViewById(R.id.button_complete_toolbar_activity_imgselect);
        mProgressbarLoadingImg = findViewById(R.id.progressbar_loadingimg_activity_imgselect);
        mProgressbarLoadingImg.setVisibility(View.VISIBLE);
        mMask = findViewById(R.id.mask_activity_imgselect);
        mPreviewPopWindowRootview = getLayoutInflater().inflate(R.layout.popwindow_selectpreview_activity_imgselect, null, false);
        mPreviewImageViewInPopWindow = mPreviewPopWindowRootview.findViewById(R.id.imageview_preview_activity_imgselect);
        mPreviewPopWindow = new PopupWindow(mPreviewPopWindowRootview, (int)(mDisplayMetrics.widthPixels * 0.8f), (int)(mDisplayMetrics.heightPixels * 0.8f));
        mFootbarButtonGroup = findViewById(R.id.buttongroup_footbar_activity_imgselect);
    }
    private void initFootbarButtonGroup(){
        mFootbarButtonGroup.addImageTextButtom(3);
        mFootbarButtonGroup.setAllTextViewConstraintHeightPercent(1);
        mFootbarButtonGroup.setAllImageViewGone();
        mClearSelectButton = mFootbarButtonGroup.getButtonAt(0);
        mClearSelectButton.getTextView().setText("清除选择");
        mClearSelectButton.getTextView().setGravity(Gravity.CENTER);
        mClearSelectButton.getTextView().setTextSize(18);
        mClearSelectButton.getTextView().setTextColor(Color.GRAY);
        mClearSelectButton.setOnClickListener((v) -> {
            if (mSelectedPosList.size() != 0){
                for (int pos : mSelectedPosList){
                    mAdapter.notifyItemChanged(pos, CLEAR_SELECT);
                }
                mSelectedPosList.clear();
                mClearSelectButton.getTextView().setTextColor(Color.GRAY);
                mClearSelectButton.getTextView().setText("清除选择");
            }
        });
    }
    private void setViewsListener(){
        mBackButton.setOnClickListener((v) -> {
            finish();
        });
        mCompleteButton.setOnClickListener((v) -> {
            ArrayList<String> uriStrings = new ArrayList<>();
            ArrayList<String> orientStrings = new ArrayList<>();
            for (int position : mSelectedPosList){
                Uri uri = mSystemPicAccesser.getImgUriByPosition(position);
                String orient = mSystemPicAccesser.getImgOrientationByPosition(position);
                uriStrings.add(uri.toString());
                orientStrings.add(orient);
            }
            Intent intent = getIntent();
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(CommonExtraTag.IMGSELECT_ACTIVITY_URI_STRINGS, uriStrings);
            bundle.putStringArrayList(CommonExtraTag.IMGSELECT_ACTIVITY_ORIENTATION_STRINGS, orientStrings);
            intent.putExtras(bundle);
            setResult(MainActivity.OPTION_IMPORT_IMG, intent);
            finish();
        });
        mMask.setOnClickListener((v) -> {
            mPreviewPopWindow.dismiss();
        });
        mPreviewPopWindow.setOnDismissListener(() -> {
            mMask.setVisibility(View.INVISIBLE);
        });
    }
    private void setItemCheckButtonClickListener(Button checkButton, int position){
        checkButton.setOnClickListener((v) -> {
            if (mSelectedPosList.contains(position)){
                mSelectedPosList.remove(Integer.valueOf(position));
                checkButton.setText("");
                checkButton.setBackgroundResource(R.drawable.button_item_check_activity_imgselect_unpress_bg);
                //mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), UPDATE_CHECK_BUTTON_ORDER);
                if (mSelectedPosList.size() == 0){
                    mClearSelectButton.getTextView().setText("清除选择");
                    mClearSelectButton.getTextView().setTextColor(Color.GRAY);
                }else{
                    mClearSelectButton.getTextView().setText(String.format("清除选择(%d)", mSelectedPosList.size()));
                }
                for (int pos : mSelectedPosList){
                    mAdapter.notifyItemChanged(pos, UPDATE_CHECK_BUTTON_ORDER);
                }
            }else{
                mSelectedPosList.add(position);
                checkButton.setText("" + mSelectedPosList.size());
                checkButton.setBackgroundResource(R.drawable.button_item_check_activity_main_press_bg);
                mClearSelectButton.getTextView().setTextColor(Color.BLACK);
                mClearSelectButton.getTextView().setText(String.format("清除选择(%d)", mSelectedPosList.size()));
            }
        });
    }
    private void setItemClickListener(View view, int position){
        view.setOnClickListener((v) -> {
            Glide.with(this).load(mSystemPicAccesser.getImgUriByPosition(position)).fitCenter().into(mPreviewImageViewInPopWindow);
            mMask.setVisibility(View.VISIBLE);
            mPreviewPopWindow.setFocusable(true);
            mPreviewPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        });
    }
}
