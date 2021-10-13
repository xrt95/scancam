package com.xrt.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.xrt.R;
import com.xrt.accesser.picinfo.PicInfoAccesser;
import com.xrt.accesser.originpic.OriginPicAccesserFactory;
import com.xrt.accesser.originpic.OriginPicAccesser;
import com.xrt.thirdpartylib.itext.PdfUtils;
import com.xrt.constant.CommonExtraTag;
import com.xrt.constant.DimenInfo;
import com.xrt.tools.IOUtils;
import com.xrt.tools.ShareTools;
import com.xrt.tools.UiTools;
import com.xrt.widget.HorizonImageTextButtonGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class FlatViewActivity extends AppCompatActivity {
    private int mType = TYPE_NORMAL;
    private RecyclerView mRecycleview;
    private RecyclerView.Adapter<ItemViewHolder> mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private DisplayMetrics mDisplayMetrics;
    private ChangeBounds mChangeBounds;
    private PicInfoAccesser mDataAccesser;
    private List<Integer> mSelectedItemPosList = new ArrayList<>();
    private OriginPicAccesser mOriginPicAccesser;
    private static final float HORIZON_GAP_OF_WIDTH_PERCENT = 0.025f;
    private static final float VERTICAL_GAP_OF_HEIGHT_PERCENT = 0.05f;
    private static final int EACH_ROW_COUNT = 2;
    private static final int SAMPLE_SIZE = 4;
    private List<String> mUpdatePicPathFromOtherActivity = new ArrayList<>();
    private List<String> mUpdatePicPathWhenAdjustOrder = new ArrayList<>();
    //intent fields
    private String mPicDirAbsPath;
    private String mTitle;
    private String mPdfDirAbspath;
    //flag field
    private int mMode = NO_MODE;
    private static final int NO_MODE = 1;
    private static final int SELECT_MODE = 2;
    private static final int MOVE_MODE = 3;
    private int mRestartState = RESTART_NORMAL;
    private static final int RESTART_NORMAL = 1;
    private static final int RESTART_FROM_ACTIVITY = 2;
    private boolean isSelectAll = false;
    private boolean mIsForceUpdatePreview = false;
    //Views fields
    private ConstraintLayout mRootView;
    private Button mBackButton;
    private TextView mToolbarTitleView;
    private Button mMenuButton;
    private PopupMenu mPopMenu;
    private ConstraintLayout mToolbar;
    private ConstraintLayout mSelectbar;
    private ConstraintLayout mFootbar;
    private ConstraintLayout mMovebar;
    private TextView mSelectbarTextView;
    private Button mSelectbarQuitButton;
    private Button mMovebarQuitButton;
    private HorizonImageTextButtonGroup mSelectFootbarButtonGroup;
    private HorizonImageTextButtonGroup.ImageTextButton mSelectFootbarMoveButton;
    private HorizonImageTextButtonGroup.ImageTextButton mSelectFootbarShareButton;
    private HorizonImageTextButtonGroup.ImageTextButton mSelectFootbarDeleteButton;
    private Button mSelectAllButton;
    private View mLoadingPdfHintRootView;
    private PopupWindow mLoadingPdfHintPopWindow;
    private View mSetPdfNameWindowRootView;
    private PopupWindow mSetPdfNameWindow;
    private PopupWindow mDeleteHintPopwindow;
    private View mDeleteHintPopwindowRootView;
    private TextView mMaskView;
    private Button mShareWholePdfButton;
    //局部刷新相关变量-多选模式
    private static final int ITEM_CLICK = 1;
    private static final int ITEM_CLICK_CANCEL = 2;
    private static final int ENTER_SELECT_MODE = 3;
    private static final int QUIT_SELECT_MODE = 4;
    private static final int SELECT_ALL = 5;
    private static final int SELECT_ALL_CANCEL = 6;
    //局部刷新相关变量-调整顺序模式
    private static final int UPDATE_CLICK_LISTENER = 7;
    //页面类型相关变量
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_START_FROM_ACTIVITY_MAIN = 2;
    //
    private static final int START_PICVIEW_ACTIVITY = 0X10;
    public static final int OPTION_UPDATE_PIC = 1;
    public static final String UPDATE_PIC_PATHS = "update_pic_paths";
    private static final int START_MOVESELECT_ACTIVITY = 0X20;
    public static final int OPTION_DELETE_MOVED_PICS = 2;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flatview);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        getOnCreateIntent();
        initNormalVar();
        initView();
        initSelectFootbarButtonGroup();
        setActivityTitle();
        setViewsListener();
        mOriginPicAccesser = OriginPicAccesserFactory.createOriginImgAccesser(this);
        mAdapter = new RecyclerViewAdapter();
        mLayoutManager = new GridLayoutManager(this, EACH_ROW_COUNT);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
        touchHelper.attachToRecyclerView(mRecycleview);
        mRecycleview.setAdapter(mAdapter);
        mRecycleview.setLayoutManager(mLayoutManager);
        ItemDecoration itemDecoration = new ItemDecoration();
        mRecycleview.addItemDecoration(itemDecoration);
        mDataAccesser = new PicInfoAccesser(this, mPicDirAbsPath);
        mDataAccesser.setPicLoadedListener((loadedPic) -> {
            mRecycleview.post(() -> {
                mAdapter.notifyItemInserted(mAdapter.getItemCount());
            });
        });
        Thread thread = new Thread(() -> {
            mDataAccesser.loadPicInfo(4);
        });
        thread.start();
    }
    @Override
    public void onRestart(){
        super.onRestart();
        switch (mRestartState){
            case RESTART_FROM_ACTIVITY:
                Thread thread1 = new Thread(() -> {
                    updateItemOnRestart(mUpdatePicPathFromOtherActivity);
                });
                thread1.start();
                break;
            case RESTART_NORMAL:
                Thread thread2 = new Thread(() -> {
                    updateItemOnRestart(mUpdatePicPathWhenAdjustOrder);
                    mUpdatePicPathWhenAdjustOrder.clear();
                });
                thread2.start();
                break;
        }
        mRestartState = RESTART_NORMAL;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == START_PICVIEW_ACTIVITY){
            mRestartState = RESTART_FROM_ACTIVITY;
            if (resultCode == OPTION_UPDATE_PIC){
                mUpdatePicPathFromOtherActivity = intent.getStringArrayListExtra(UPDATE_PIC_PATHS);
            }
        }
        else if (requestCode == START_MOVESELECT_ACTIVITY){
            if (resultCode == OPTION_DELETE_MOVED_PICS){
                deleteOption();
            }
        }
    }
    private void setViewsListener(){
        mBackButton.setOnClickListener((view) -> {
            if ( mType == TYPE_START_FROM_ACTIVITY_MAIN && mAdapter.getItemCount() == 0){
                setResult(MainActivity.OPTION_DELETE_ITEM, getIntent());
            }
            if (mType == TYPE_START_FROM_ACTIVITY_MAIN && mIsForceUpdatePreview){
                setResult(MainActivity.FORCE_UPDATE_PREVIEW, getIntent());
            }
            finish();
        });
        mShareWholePdfButton.setOnClickListener((view) -> {
            generateAndShareWholePdf(ShareTools.SHARE_MORE);
        });
        mMenuButton.setOnClickListener((view) -> {
            mPopMenu.show();
        });
        mPopMenu.setOnMenuItemClickListener((item) -> {
            switch(item.getItemId()){
                case R.id.option_adjust_order_activity_flatview:
                    enterMoveOrderInterface();
                    break;
            }
            return true;
        });
        mSelectbarQuitButton.setOnClickListener((view) -> {
            quitSelectInterface();
        });
        mMovebarQuitButton.setOnClickListener((view) -> {
            quitMoveOrderInterface();
        });
        mSelectAllButton.setOnClickListener((view) -> {
            if (isSelectAll){
                isSelectAll = false;
                mSelectAllButton.setBackgroundResource(R.drawable.selectall_button_black);
                mSelectedItemPosList.clear();
                mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), SELECT_ALL_CANCEL);
            }else{
                isSelectAll = true;
                mSelectAllButton.setBackgroundResource(R.drawable.selectall_button_blue);
                mSelectedItemPosList.clear();
                for (int i = 0; i < mAdapter.getItemCount(); i++){
                    mSelectedItemPosList.add(i);
                }
                mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), SELECT_ALL);
            }
        });
    }
    private void updateItemOnRestart(List<String> needToUpdatePicPaths){
        Object[] result = mDataAccesser.updatePicInfo(SAMPLE_SIZE, needToUpdatePicPaths);
        int delta = (int)result[0];
        boolean isUpdate = (boolean)result[1];
        if (delta < 0){
            mRecycleview.post(() -> {
                for (int i = 0; i < Math.abs(delta); i++){
                    mAdapter.notifyItemRemoved(0);
                }
            });
        }
        if (delta != 0 || isUpdate || true) {
            mRecycleview.post(() -> {
                mAdapter.notifyDataSetChanged();//这行代码的作用是让RecyclerView所有View重新刷新一次，但是和notifyRangeChange的刷新不同，相同的数据刷新不会出现闪烁（推测是因为notifyDataSetChanged没有执行刷新动画）
            });
        }
        mIsForceUpdatePreview = judgeIsForceUpdatePreview();
    }
    private boolean judgeIsForceUpdatePreview(){
        Map<String, Integer> orderMap = mDataAccesser.getPicInfosOrderMap();
        for (int i = 0; i < mUpdatePicPathFromOtherActivity.size(); i++){
            String path = mUpdatePicPathFromOtherActivity.get(i);
            int order = orderMap.get(path);
            if (order == 0){
                return true;
            }
        }
        return false;
    }
    private void initView(){
        mRootView = findViewById(R.id.rootview_activity_flatview);
        mMaskView = findViewById(R.id.mask_activity_flatview);
        mRecycleview = findViewById(R.id.recycleView_activity_flatview);
        mBackButton = findViewById(R.id.button_back_activity_flatview);
        mToolbarTitleView = findViewById(R.id.title_toolbar_activity_flatview);
        mMenuButton = findViewById(R.id.button_menu_toolbar_activity_flatview);
        mPopMenu = new PopupMenu(this, mMenuButton);
        mPopMenu.inflate(R.menu.menu_toolbar_activity_flatview);
        mToolbar = findViewById(R.id.toolbar_activity_flatview);
        mSelectbar = findViewById(R.id.selectbar_activity_flatview);
        mFootbar = findViewById(R.id.footbar_activity_flatview);
        mMovebar = findViewById(R.id.movebar_activity_flatview);
        mSelectbarTextView = findViewById(R.id.textview_selectbar_activity_flatview);
        mSelectbarQuitButton = findViewById(R.id.button_quit_selectbar_activity_flatview);
        mMovebarQuitButton = findViewById(R.id.button_quit_movebar_activity_flatview);
        mSelectFootbarButtonGroup = findViewById(R.id.buttongroup_footbar_activity_flatview);
        mSelectAllButton = findViewById(R.id.button_selectall_selectbar_activity_flatview);
        mLoadingPdfHintRootView = getLayoutInflater().inflate(R.layout.popwindow_loadingpdf_hint, null);
        mLoadingPdfHintPopWindow = new PopupWindow(mLoadingPdfHintRootView, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        mSetPdfNameWindowRootView = getLayoutInflater().inflate(R.layout.popwindow_sharepdf_setname_activity_flatview, null);
        int renamePopWindowWidth = (int)(mDisplayMetrics.widthPixels * DimenInfo.RENAME_POPWINDOW_WIDTH_PECENT);
        int renamePopWindowHeight = UiTools.dpTopx(this, DimenInfo.RENAME_POPWINDOW_HEIGHT);
        mSetPdfNameWindow = new PopupWindow(mSetPdfNameWindowRootView, renamePopWindowWidth, renamePopWindowHeight);
        mDeleteHintPopwindowRootView = getLayoutInflater().inflate(R.layout.popwindow_delete_hint, null);
        int deleteHintPopwindowHeight = getResources().getDimensionPixelSize(R.dimen.popwindow_delete_hint_height);
        mDeleteHintPopwindow = new PopupWindow(mDeleteHintPopwindowRootView, mDisplayMetrics.widthPixels, deleteHintPopwindowHeight);
        mShareWholePdfButton = findViewById(R.id.button_share_whole_pdf_activity_flatview);
    }
    private void initNormalVar(){
        mDisplayMetrics = UiTools.getScreenMetrics(this);
        mChangeBounds = new ChangeBounds();
        mChangeBounds.setDuration(200);
    }
    private void initSelectFootbarButtonGroup(){
        mSelectFootbarButtonGroup.addImageTextButtom(3);
        mSelectFootbarMoveButton = mSelectFootbarButtonGroup.getButtonAt(0);
        mSelectFootbarMoveButton.getImageView().setImageResource(R.drawable.share_button);
        mSelectFootbarMoveButton.getTextView().setText("移动");
        mSelectFootbarMoveButton.getTextView().setTextColor(Color.parseColor("black"));
        mSelectFootbarShareButton = mSelectFootbarButtonGroup.getButtonAt(1);
        mSelectFootbarShareButton.getImageView().setImageResource(R.drawable.share_button);
        mSelectFootbarShareButton.getTextView().setText("分享");
        mSelectFootbarShareButton.getTextView().setTextColor(Color.parseColor("black"));
        mSelectFootbarDeleteButton = mSelectFootbarButtonGroup.getButtonAt(2);
        mSelectFootbarDeleteButton.getImageView().setImageResource(R.drawable.delete_button);
        mSelectFootbarDeleteButton.getTextView().setText("删除");
        mSelectFootbarDeleteButton.getTextView().setTextColor(Color.parseColor("black"));
        mSelectFootbarMoveButton.setOnClickListener((view) -> {
            if (mSelectedItemPosList.size() == 0){
                Toast.makeText(this, R.string.multi_pic_select_error, Toast.LENGTH_SHORT).show();
                return;
            }
            startMoveSelectActivity(mPicDirAbsPath);
        });
        mSelectFootbarShareButton.setOnClickListener((view) -> {
            if (mSelectedItemPosList.size() == 0){
                Toast.makeText(this, R.string.multi_pic_select_error, Toast.LENGTH_SHORT).show();
                return;
            }
            popSetPdfNameWindow();
        });
        mSelectFootbarDeleteButton.setOnClickListener((view) -> {
            if (mSelectedItemPosList.size() == 0){
                Toast.makeText(this, R.string.multi_pic_select_error, Toast.LENGTH_SHORT).show();
            }else{
                popDeleteHintPopWindow(mMaskView,
                        v -> {
                            deleteOption();
                            mDeleteHintPopwindow.dismiss();
                        },
                        v -> {mDeleteHintPopwindow.dismiss();
                        });
            }
        });
    }
    private void deleteOption(){
        deletePics();
        mDataAccesser.updatePreferences(mDataAccesser.getPicInfosOrderMap());
        mAdapter.notifyItemRangeRemoved(0, mSelectedItemPosList.size());
        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
        quitSelectInterface();
    }
    private ArrayList<String> getSelectedPicAbsPaths(){
        List<PicInfoAccesser.PicInfo> picInfos = mDataAccesser.getPicInfos();
        ArrayList<String> selectedPicPaths = new ArrayList<>();
        Collections.sort(mSelectedItemPosList);
        for (int i = 0; i < mSelectedItemPosList.size(); i++){
            int pos = mSelectedItemPosList.get(i);
            selectedPicPaths.add(picInfos.get(pos).getPath());
        }
        return selectedPicPaths;
    }
    private void deletePics(){
        List<PicInfoAccesser.PicInfo> picInfos = mDataAccesser.getPicInfos();
        List<PicInfoAccesser.PicInfo> picsToDel = new ArrayList<>();
        for (int i = 0; i < mSelectedItemPosList.size(); i++){
            int posToDel = mSelectedItemPosList.get(i);
            picsToDel.add(picInfos.get(posToDel));
        }
        for (PicInfoAccesser.PicInfo picInfo : picsToDel){
            String picPath = picInfo.getPath();
            String relativePath = picPath.replace(getExternalFilesDir("").getPath(), "");
            mOriginPicAccesser.deleteOriginPic(relativePath);
            IOUtils.deleteDirectoryOrFile(picPath);
        }
        picInfos.removeAll(picsToDel);
    }
    private void popSetPdfNameWindow(){
        EditText editText = mSetPdfNameWindowRootView.findViewById(R.id.edittext_pdfsetname_popwindow_activity_flatview);
        TextView okButton = mSetPdfNameWindowRootView.findViewById(R.id.button_confirm_pdfsetname_popwindow_activity_flatview);
        TextView cancelButton = mSetPdfNameWindowRootView.findViewById(R.id.cancel_button_pdfsetname_popwindow_activity_flatview);
        editText.getText().clear();
        okButton.setOnClickListener((view) -> {
            String title = editText.getText().toString();
            shareSelectedImgWithPdf(title);
            mSetPdfNameWindow.dismiss();
        });
        cancelButton.setOnClickListener((view) -> {
            String title = "新建文档_" + System.currentTimeMillis();
            shareSelectedImgWithPdf(title);
            mSetPdfNameWindow.dismiss();
        });
        mSetPdfNameWindow.setFocusable(true);//这行不能少，否则重命名弹窗中的输入框无法获得焦点
        mSetPdfNameWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        popSetNameKeyBoard();
        mSetPdfNameWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        editText.requestFocus();
    }
    private void popSetNameKeyBoard(){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
    }
    private void shareSelectedImgWithPdf(String pdfTitle){
        List<String> imgAbsPaths = new ArrayList<>();
        List<PicInfoAccesser.PicInfo> picInfoList = mDataAccesser.getPicInfos();
        Collections.sort(mSelectedItemPosList);
        for (int i = 0; i < mSelectedItemPosList.size(); i++){
            int position = mSelectedItemPosList.get(i);
            imgAbsPaths.add(picInfoList.get(position).getPath());
        }
        String pdfOutputPath = mPdfDirAbspath + File.separator + pdfTitle + ".pdf";
        mLoadingPdfHintPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        Thread thread = new Thread(() -> {
            PdfUtils.imgToPdf(imgAbsPaths, pdfOutputPath, 20);
            mRootView.post(() -> {
                mLoadingPdfHintPopWindow.dismiss();
                quitSelectInterface();
            });
            try{
                ShareTools.shareFile(pdfOutputPath, "application/pdf", this, "com.xrt.fileprovider", ShareTools.SHARE_MORE);
            }catch (Exception e){
                mRootView.post(() -> {
                    Toast.makeText(mRootView.getContext(), R.string.share_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
        thread.start();
    }
    private void getOnCreateIntent(){
        Intent intent = getIntent();
        mPicDirAbsPath = intent.getStringExtra(CommonExtraTag.FLATVIEW_ACTIVITY_PICABSPATH);
        mTitle = intent.getStringExtra(CommonExtraTag.FLATVIEW_ACTIVITY_TITLE);
        mPdfDirAbspath = intent.getStringExtra(CommonExtraTag.FLATVIEW_ACTIVITY_PDFDIR_ABSPATH);
        mType = intent.getIntExtra(CommonExtraTag.FLATVIEW_ACTIVITY_TYPE, TYPE_NORMAL);
    }
    private void setActivityTitle(){
        mToolbarTitleView.setText(mTitle);
    }

    public class ItemTouchHelperCallback extends ItemTouchHelper.Callback{
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder){
            int dragFlags = 0;
            if (mMode == MOVE_MODE){
                dragFlags =  ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT | ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            }
            int flags = makeMovementFlags(dragFlags, 0);
            return flags;
        }
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder sourceViewHolder, RecyclerView.ViewHolder targetViewHolder){
            int sourcePosition = sourceViewHolder.getAdapterPosition();
            int targetPosition = targetViewHolder.getAdapterPosition();
            List<PicInfoAccesser.PicInfo> picInfos = mDataAccesser.getPicInfos();
            for(int i = Math.min(sourcePosition, targetPosition); i <= Math.max(sourcePosition, targetPosition); i++){
                mUpdatePicPathWhenAdjustOrder.add(picInfos.get(i).getPath());
            }
            mAdapter.notifyItemMoved(sourcePosition, targetPosition);//这行代码只是执行动画
            return true;
        }
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction){
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder{
        private ConstraintLayout rootView;
        private ImageView imageView;
        private TextView checkTextView;
        private TextView countTextView;
        private int position;

        public ItemViewHolder(View view){
            super(view);
            imageView = view.findViewById(R.id.imageview_item_recyclerview_activity_flatview);
            rootView = view.findViewById(R.id.rootview_item_recyclerview_activity_flatview);
            checkTextView = view.findViewById(R.id.textview_item_check_activity_flatview);
            countTextView = view.findViewById(R.id.textview_item_count_activity_flatview);
        }
    }
    public class ItemDecoration extends RecyclerView.ItemDecoration{
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView recyclerView, RecyclerView.State state){
            int position = recyclerView.getLayoutManager().getPosition(view);
            int r = position % EACH_ROW_COUNT;
            int baseWidth = mDisplayMetrics.widthPixels;
            switch(r){
                case 0:
                    outRect.top = getVerticalGapHeight(baseWidth) * 2;
                    outRect.left = getHorizonGapWidth(baseWidth);
                    outRect.right = getHorizonGapWidth(baseWidth);
                    break;
                case EACH_ROW_COUNT - 1:
                    outRect.top = getVerticalGapHeight(baseWidth) * 2;
                    outRect.left = getHorizonGapWidth(baseWidth);
                    outRect.right = getHorizonGapWidth(baseWidth);
                    break;
            }
        }
    }
    public class RecyclerViewAdapter extends RecyclerView.Adapter<ItemViewHolder>{
        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parentView, int viewType){
            View view = getLayoutInflater().inflate(R.layout.item_recyclerview_activity_flatview, parentView, false);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.height = getItemViewHeight(parentView.getWidth());
            return new ItemViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position){
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position, List<Object> payLoads){
            if (payLoads.isEmpty()){
                //Log.d("mxrt", "entered");
                viewHolder.imageView.setImageBitmap(mDataAccesser.getPicInfos().get(position).getBitmap());
                viewHolder.countTextView.setText("" + (position + 1));
                viewHolder.position = position;
                //这里负责滚动时的渲染
                if (mMode == SELECT_MODE){
                    viewHolder.checkTextView.setBackgroundResource(R.drawable.textview_item_check_activity_flatview_unpress_bg);
                    viewHolder.checkTextView.setVisibility(View.VISIBLE);
                    if (mSelectedItemPosList.contains(position)){
                        viewHolder.checkTextView.setBackgroundResource(R.drawable.textview_item_check_activity_flatview_press_bg);
                    }
                }else{
                    viewHolder.checkTextView.setVisibility(View.INVISIBLE);
                }
            }else{
                //这里负责主动触发的渲染
                for (int i = 0; i < payLoads.size(); i++){
                    int flag = (int)payLoads.get(i);
                    switch(flag){
                        case ENTER_SELECT_MODE:
                            viewHolder.checkTextView.setBackgroundResource(R.drawable.textview_item_check_activity_flatview_unpress_bg);
                            viewHolder.checkTextView.setVisibility(View.VISIBLE);
                            break;
                        case QUIT_SELECT_MODE:
                            viewHolder.checkTextView.setVisibility(View.INVISIBLE);
                            break;
                        case ITEM_CLICK:
                            viewHolder.checkTextView.setBackgroundResource(R.drawable.textview_item_check_activity_flatview_press_bg);
                            break;
                        case ITEM_CLICK_CANCEL:
                            viewHolder.checkTextView.setBackgroundResource(R.drawable.textview_item_check_activity_flatview_unpress_bg);
                            break;
                        case SELECT_ALL:
                            viewHolder.checkTextView.setBackgroundResource(R.drawable.textview_item_check_activity_flatview_press_bg);
                            break;
                        case SELECT_ALL_CANCEL:
                            viewHolder.checkTextView.setBackgroundResource(R.drawable.textview_item_check_activity_flatview_unpress_bg);
                            break;
                        case UPDATE_CLICK_LISTENER:
                            viewHolder.position = position;
                            viewHolder.countTextView.setText("" + (position + 1));
                            updateItemClickListener(viewHolder.rootView, position);
                            updateItemLongClickListener(viewHolder.rootView, viewHolder, position);
                            break;
                    }
                }
            }
            updateItemClickListener(viewHolder.rootView, position);
            updateItemLongClickListener(viewHolder.rootView, viewHolder, position);
        }
        @Override
        public int getItemCount(){
            return mDataAccesser.getPicInfos().size();
        }
    }
    public void updateItemClickListener(View view, int position){
        view.setOnClickListener((v) -> {
            if (mMode == SELECT_MODE){
                selectItem(position);
            }else if(mMode == MOVE_MODE){
            }else {
                startPicviewActivity(position);
            }
        });
    }
    public void updateItemLongClickListener(View view, ItemViewHolder viewHolder, int position){
        view.setOnLongClickListener((v) -> {
            enterSelectInterface();
            return true;
        });
    }
    private void selectItem(int position){
        if (mSelectedItemPosList.contains(position)){
            mSelectedItemPosList.remove(Integer.valueOf(position));
            mAdapter.notifyItemChanged(position, ITEM_CLICK_CANCEL);
            mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
        }else{
            mSelectedItemPosList.add(position);
            mAdapter.notifyItemChanged(position, ITEM_CLICK);
            mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent){
        if (keyCode == KeyEvent.KEYCODE_BACK){
            switch (mMode){
                case SELECT_MODE:
                    quitSelectInterface();
                    return true;
                case MOVE_MODE:
                    quitMoveOrderInterface();
                    return true;
            }
            if (mType == TYPE_START_FROM_ACTIVITY_MAIN && mAdapter.getItemCount() == 0){
                setResult(MainActivity.OPTION_DELETE_ITEM, getIntent());
            }
            if (mType == TYPE_START_FROM_ACTIVITY_MAIN && mIsForceUpdatePreview){
                setResult(MainActivity.FORCE_UPDATE_PREVIEW, getIntent());
            }
        }
        return super.onKeyDown(keyCode, keyEvent);
    }

    private void enterSelectInterface(){
        if (mMode == NO_MODE){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeToolbarState(false);
            changeSelectbarState(false);
            changeFootbarState(false);
            changeCheckViewState(false);
            mMode = SELECT_MODE;
        }
    }
    private void enterMoveOrderInterface(){
        if (mMode == NO_MODE){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeToolbarState(false);
            changeMovebarState(false);
            mMode = MOVE_MODE;
        }
    }
    private void quitSelectInterface(){
        if (mMode == SELECT_MODE){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeToolbarState(true);
            changeSelectbarState(true);
            changeFootbarState(true);
            changeCheckViewState(true);
            mSelectedItemPosList.clear();
            mSelectbarTextView.setText("已选择 项");
            mSelectAllButton.setBackgroundResource(R.drawable.selectall_button_black);
            isSelectAll = false;
            mMode = NO_MODE;
        }
    }
    private void quitMoveOrderInterface(){
        if (mMode == MOVE_MODE){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeToolbarState(true);
            changeMovebarState(true);
            updateOrder();
            mMode = NO_MODE;
        }
    }
    private void changeToolbarState(boolean isRestore){
        ViewGroup.LayoutParams lp = mToolbar.getLayoutParams();
        if (isRestore){
            lp.height = getResources().getDimensionPixelSize(R.dimen.toolbar_activity_flatview_height);
        }else{
            lp.height = 0;
        }
        mToolbar.setLayoutParams(lp);
    }
    private void changeSelectbarState(boolean isRestore){
        ConstraintSet cs = new ConstraintSet();
        cs.clone(mRootView);
        ViewGroup.LayoutParams lp = mSelectbar.getLayoutParams();
        if (isRestore){
            cs.clear(mRecycleview.getId(), ConstraintSet.TOP);
            cs.connect(mRecycleview.getId(), ConstraintSet.TOP, mToolbar.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            lp.height = 0;
        }else{
            cs.clear(mRecycleview.getId(), ConstraintSet.TOP);
            cs.connect(mRecycleview.getId(), ConstraintSet.TOP, mSelectbar.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            lp.height = getResources().getDimensionPixelSize(R.dimen.selectbar_activity_flatview_height);
        }
        mSelectbar.setLayoutParams(lp);
    }
    private void changeFootbarState(boolean isRestore){
        ConstraintSet cs = new ConstraintSet();
        cs.clone(mRootView);
        ViewGroup.LayoutParams lp = mFootbar.getLayoutParams();
        if (isRestore){
            cs.clear(mRecycleview.getId(), ConstraintSet.BOTTOM);
            cs.connect(mRecycleview.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            lp.height = 0;
        }else{
            cs.clear(mRecycleview.getId(), ConstraintSet.BOTTOM);
            cs.connect(mRecycleview.getId(), ConstraintSet.BOTTOM, mFootbar.getId(), ConstraintSet.TOP);
            cs.applyTo(mRootView);
            lp.height = getResources().getDimensionPixelSize(R.dimen.footbar_activity_flatview_height);
        }
        mFootbar.setLayoutParams(lp);
    }
    private void changeMovebarState(boolean isRestore){
        ConstraintSet cs = new ConstraintSet();
        cs.clone(mRootView);
        ViewGroup.LayoutParams lp = mMovebar.getLayoutParams();
        if (isRestore){
            cs.clear(mRecycleview.getId(), ConstraintSet.TOP);
            cs.connect(mRecycleview.getId(), ConstraintSet.TOP, mToolbar.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            lp.height = 0;
        }else{
            cs.clear(mRecycleview.getId(), ConstraintSet.TOP);
            cs.connect(mRecycleview.getId(), ConstraintSet.TOP, mMovebar.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            lp.height = getResources().getDimensionPixelSize(R.dimen.selectbar_activity_flatview_height);
        }
        mMovebar.setLayoutParams(lp);
    }
    private void changeCheckViewState(boolean isRestore){
        if (isRestore){
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), QUIT_SELECT_MODE);
        }else{
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), ENTER_SELECT_MODE);
        }
    }
    private void startMoveSelectActivity(String scanDirAbsPath){
        Intent intent = new Intent(CommonExtraTag.START_MOVESELECT_ACTIVITY_ACTION);
        Bundle bundle = new Bundle();
        bundle.putInt(CommonExtraTag.MOVESELECT_ACTIVITY_TYPE, MoveSelectActivity.TYPE_START_FROM_ACTIVITY_FLATVIEW);
        bundle.putString(CommonExtraTag.MOVESELECT_SCAN_DIR_ABS_PATH, scanDirAbsPath);
        bundle.putStringArrayList(CommonExtraTag.MOVESELECT_PIC_ABS_PATHS, getSelectedPicAbsPaths());
        intent.putExtras(bundle);
        startActivityForResult(intent, START_MOVESELECT_ACTIVITY);
    }
    private void startPicviewActivity(int picIndex){
        Intent intent = new Intent(CommonExtraTag.START_PICVIEW_ACTIVITY_ACTION);
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_TYPE, PictureViewActivity.TYPE_START_FROM_FLATVIEW);
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILEPATH_EXTRA, mPicDirAbsPath);
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_FIRST_LOAD_PIC_INDEX, picIndex);
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILE_TITLE, mTitle);
        startActivityForResult(intent, START_PICVIEW_ACTIVITY);
    }
    public int getHorizonGapWidth(int baseWidth){
        return (int)(baseWidth * HORIZON_GAP_OF_WIDTH_PERCENT);
    }
    public int getVerticalGapHeight(int baseWidth){
        return getHorizonGapWidth(baseWidth);
    }
    public int getItemViewWidth(int baseWidth){
        return (baseWidth - getHorizonGapWidth(baseWidth) * (EACH_ROW_COUNT + 1)) / EACH_ROW_COUNT;
    }
    public int getItemViewHeight(int baseWidth){
        return (int)(getItemViewWidth(baseWidth) * 1.3f);
    }
    private void updateOrder(){
        Map<String,Integer> newPathOrderMap = getPicPathOrderMap();
        mDataAccesser.updatePreferences(newPathOrderMap);
        mDataAccesser.updatePicInfo(SAMPLE_SIZE, null);
        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), UPDATE_CLICK_LISTENER);
    }
    private Map<String, Integer> getPicPathOrderMap(){
        Map<String, Integer> resultMap = new HashMap<>();
        List<PicInfoAccesser.PicInfo> picInfos = mDataAccesser.getPicInfos();
        for (int i = 0; i < mRecycleview.getChildCount(); i++){
            View view = mRecycleview.getLayoutManager().getChildAt(i);
            int adapterPos = ((ItemViewHolder)mRecycleview.getChildViewHolder(view)).position;
            String path = picInfos.get(adapterPos).getPath();
            resultMap.put(path, i);
        }
        return resultMap;
    }
    private void popDeleteHintPopWindow(View maskView, View.OnClickListener okListener, View.OnClickListener cancelListener){
        if (maskView != null){
            maskView.setVisibility(View.VISIBLE);
            mDeleteHintPopwindow.setOnDismissListener(() -> {
                maskView.setVisibility(View.GONE);
            });
        }
        Button okButton = mDeleteHintPopwindowRootView.findViewById(R.id.button_ok_delete_hint_popwindow);
        Button cancelButton = mDeleteHintPopwindowRootView.findViewById(R.id.button_cancel_delete_hint_popwindow);
        okButton.setOnClickListener(okListener);
        cancelButton.setOnClickListener(cancelListener);
        mDeleteHintPopwindow.setFocusable(true);
        mDeleteHintPopwindow.showAtLocation(mRootView, Gravity.BOTTOM, 0, 0);
    }
    private void generateAndShareWholePdf(int channel){
        mLoadingPdfHintPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        String pdfStorePath = mPdfDirAbspath + File.separator + mTitle + ".pdf";
        Thread thread = new Thread(() -> {
            List<String> picPaths = mDataAccesser.getOrderAdjustedPicAbsPaths();
            PdfUtils.imgToPdf(picPaths, pdfStorePath,20);
            mRootView.post(() -> {
               mLoadingPdfHintPopWindow.dismiss();
            });
            try{
                ShareTools.shareFile(pdfStorePath, "application/pdf", this, "com.xrt.fileprovider", channel);
            }catch (Exception e){
                Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show();
            }
        });
        thread.start();
    }
}
