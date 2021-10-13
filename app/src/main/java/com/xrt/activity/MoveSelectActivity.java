package com.xrt.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.xrt.R;
import com.xrt.constant.CommonExtraTag;
import com.xrt.constant.StringConstant;
import com.xrt.func.movepic.Mover;
import com.xrt.func.movepic.MoverFactory;
import com.xrt.accesser.originpic.OriginPicAccesser;
import com.xrt.accesser.originpic.OriginPicAccesserFactory;
import com.xrt.accesser.scanpic.ScanPicAccesser;
import com.xrt.accesser.scanpic.ScanPicAccesserFactory;
import com.xrt.tools.UiTools;
import com.xrt.widget.ConfirmWindowPoper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MoveSelectActivity extends AppCompatActivity {
    private RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;
    private ScanPicAccesser mScanPicAccesser;
    private List<String> mScanItemDirNames;
    private List<Bitmap> mLoadedPreviewPics = new ArrayList<>();
    private DisplayMetrics mDisplayMetrics;
    private OriginPicAccesser mOriginPicAcceser;
    //intent fields
    private String mCurrentScanDirAbsPath;
    private String mCurrentScanDirName;
    private List<String> mSelectedPicAbsPaths;
    //activity type fields
    private int mType;
    public static final int TYPE_START_FROM_ACTIVITY_FLATVIEW = 1;
    //views fields
    private View mRootView;
    private RecyclerView mRecyclerView;
    private Button mBackButton;
    private View mMovingPicPopWindowRootView;
    private PopupWindow mMovingPicPopWindow;
    private ConfirmWindowPoper mConfirmWindowPoper;

    //Constant
    private static final int VIEWTYPE_CREATE = 1;
    private static final int VIEWTYPE_SCANITEM = 2;

    private static final int PREVIEW_INSAMPLE_SIZE = 4;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moveselect);
        getIntentOnCreate();
        initNormalVar();
        initVarAboutScanPic();
        initVarAboutOriginPic();
        initView();
        initVarAboutScanPic();
        mAdapter = new RecyclerViewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(decoration);
        setViewsClickListener();
        loadMoveSelectItem();
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        mScanPicAccesser.reScan();
    }
    private void initVarAboutOriginPic(){
        mOriginPicAcceser = OriginPicAccesserFactory.createOriginImgAccesser(this);
    }
    private void initVarAboutScanPic(){
        mScanPicAccesser = ScanPicAccesserFactory.createScanPicAccesser(this);
        mScanItemDirNames = mScanPicAccesser.getScanItemDirNames();
    }
    private void loadMoveSelectItem(){
        switch (mType){
            case TYPE_START_FROM_ACTIVITY_FLATVIEW:
                loadScanItemWithPreivewPic(mCurrentScanDirName);
                break;
        }
    }
    private void loadScanItemWithPreivewPic(String excludeScanItemDirName){
        Thread thread = new Thread(() -> {
            List<String> newScanItemDirNames = new ArrayList<>();
            for (String scanItemName : mScanItemDirNames){
                if (!scanItemName.equals(excludeScanItemDirName)){
                    mLoadedPreviewPics.add(mScanPicAccesser.getScanItemPreviewPic(scanItemName, PREVIEW_INSAMPLE_SIZE));
                    mRecyclerView.post(() -> {
                        mAdapter.notifyItemInserted(mAdapter.getItemCount());
                    });
                    newScanItemDirNames.add(scanItemName);
                }
            }
            mScanItemDirNames = newScanItemDirNames;
        });
        thread.start();
    }
    private void getIntentOnCreate(){
        Intent intent = getIntent();
        mType = intent.getIntExtra(CommonExtraTag.MOVESELECT_ACTIVITY_TYPE, TYPE_START_FROM_ACTIVITY_FLATVIEW);
        switch (mType){
            case TYPE_START_FROM_ACTIVITY_FLATVIEW:
                mCurrentScanDirAbsPath = intent.getStringExtra(CommonExtraTag.MOVESELECT_SCAN_DIR_ABS_PATH);
                mCurrentScanDirName = new File(mCurrentScanDirAbsPath).getName();
                mSelectedPicAbsPaths = intent.getStringArrayListExtra(CommonExtraTag.MOVESELECT_PIC_ABS_PATHS);
                break;
        }
    }
    private void initNormalVar(){
        mDisplayMetrics = UiTools.getScreenMetrics(this);
    }
    private void initView(){
        mRootView = findViewById(R.id.rootview_activity_moveselect);
        mBackButton = findViewById(R.id.button_back_activity_moveselect);
        mRecyclerView = findViewById(R.id.recyclerview_activity_moveselect);
        mMovingPicPopWindowRootView = getLayoutInflater().inflate(R.layout.popwindow_movingpic_hint, null);
        mMovingPicPopWindow = new PopupWindow(mMovingPicPopWindowRootView, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        mConfirmWindowPoper = new ConfirmWindowPoper(this, R.layout.popwindow_confirm_common_use, R.id.textview_title_common_popwindow, R.id.textview_content_common_popwindow, R.id.button_confirm_common_popwindow, R.id.button_cancel_common_popwindow);
        //mInputWindowPoper = new InputWindowPoper(this, R.layout.popwindow_input_common_use, R.id.textview_title_common_input_popwindow, R.id.edittext_common_input_popwindow, R.id.button_confirm_common_input_popwindow, R.id.cancel_button_common_input_popwindow);
    }
    private void setViewsClickListener(){
        mBackButton.setOnClickListener((view) -> {
            finish();
        });
    }
    private void updateCreteItemClickListener(View view){
        view.setOnClickListener((v) -> {

            popConfirmWindow("新建目录",
                (confirm) -> {
                    mMovingPicPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
                    Thread thread = new Thread(() -> {
                        creatAndMovePic();
                        mRootView.post(() -> {
                            mMovingPicPopWindow.dismiss();
                            setResult(FlatViewActivity.OPTION_DELETE_MOVED_PICS, getIntent());
                            finish();
                        });
                    });
                    thread.start();
                },
                (cancel) -> {
                    mConfirmWindowPoper.dismissWindow();
                });

        });
    }
    private void updateItemClickListener(View view, int position, String title){
        view.setOnClickListener((v) -> {
            switch (mType){
                case TYPE_START_FROM_ACTIVITY_FLATVIEW:
                    popConfirmWindow(title,
                    (confirm) -> {
                        mMovingPicPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
                        Thread thread = new Thread(() -> {
                            movePic(position);
                            mRootView.post(() -> {
                                mMovingPicPopWindow.dismiss();
                                setResult(FlatViewActivity.OPTION_DELETE_MOVED_PICS, getIntent());
                                finish();
                            });
                        });
                        thread.start();
                    },
                    (cancel) -> {mConfirmWindowPoper.dismissWindow(); });
                    break;
            }
        });
    }
    private void creatAndMovePic(){
        String dstDirName = StringConstant.SCAN_FILENAME_START_WITH + System.currentTimeMillis();
        Mover mover = MoverFactory.createMover(this, mCurrentScanDirName, dstDirName);
        for (String srcPicPath : mSelectedPicAbsPaths){
            File picFile = new File(srcPicPath);
            String srcPicName = picFile.getName();
            mover.moveScanPic(srcPicName, srcPicName);
            String srcPicRelativePath = File.separator + mCurrentScanDirName + File.separator + srcPicName;
            if (mOriginPicAcceser.isOriginPicExist(srcPicRelativePath)){
                //Log.d("mxrt", "isEx");
                mover.moveOriginPic(srcPicName, srcPicName);
            }
        }
        //mScanPicAccesser.reScan();
    }
    private void movePic(int position){
        String dstDirName = mScanItemDirNames.get(position);
        Mover mover = MoverFactory.createMover(this, mCurrentScanDirName, dstDirName);
        for (String srcPicPath : mSelectedPicAbsPaths){
            File picFile = new File(srcPicPath);
            String srcPicName = picFile.getName();
            mover.moveScanPic(srcPicName, srcPicName);
            String srcPicRelativePath = File.separator + mCurrentScanDirName + File.separator + srcPicName;
            if(mOriginPicAcceser.isOriginPicExist(srcPicRelativePath)){
                mover.moveOriginPic(srcPicName, srcPicName);
            }
        }
    }
    private void popConfirmWindow(String itemTitle, View.OnClickListener confirmListener, View.OnClickListener cancelListener){
        mConfirmWindowPoper.popWindow("是否移动到", itemTitle, confirmListener, cancelListener);
    }
    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
        @Override
        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parentView, int viewType){
            View view;
            switch (viewType){
                case VIEWTYPE_CREATE:
                    view = getLayoutInflater().inflate(R.layout.item_create_recyclerview_activity_moveselect, parentView, false);
                    return new CreateItemViewHolder(view);
                case VIEWTYPE_SCANITEM:
                default:
                    view = getLayoutInflater().inflate(R.layout.item_recyclerview_activity_moveselect, parentView, false);
                    return new ItemViewHolder(view);
            }
        }
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position){}
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position, List<Object> payLoads){
            if (payLoads.isEmpty()){
                switch (viewHolder.getItemViewType()){
                    case VIEWTYPE_CREATE:
                        CreateItemViewHolder createItemViewHolder = (CreateItemViewHolder)viewHolder;
                        updateCreteItemClickListener(createItemViewHolder.rootView);
                        break;
                    case VIEWTYPE_SCANITEM:
                        int newPosition = position - 1;
                        ItemViewHolder itemViewHolder = (ItemViewHolder)viewHolder;
                        itemViewHolder.textView.setText(mScanPicAccesser.getScanItemTitle(mScanItemDirNames.get(newPosition)));
                        itemViewHolder.imageView.setImageBitmap(mLoadedPreviewPics.get(newPosition));
                        updateItemClickListener(itemViewHolder.rootView, newPosition, itemViewHolder.textView.getText().toString());
                        break;
                }
            }
        }
        @Override
        public int getItemCount(){
            return mLoadedPreviewPics.size() + 1;
        }
        @Override
        public int getItemViewType(int position) {
            if (position == 0){
                return VIEWTYPE_CREATE;
            }else{
                return VIEWTYPE_SCANITEM;
            }
        }
    }
    private class ItemViewHolder extends RecyclerView.ViewHolder{
        private TextView textView;
        private ImageView imageView;
        private View rootView;

        public ItemViewHolder(View view){
            super(view);
            textView = view.findViewById(R.id.textview_item_title_activity_moveselect);
            imageView = view.findViewById(R.id.imageview_item_preview_activity_moveselect);
            rootView = view.findViewById(R.id.rootview_item_recyclerview_activity_moveselect);
        }
    }
    private class CreateItemViewHolder extends  RecyclerView.ViewHolder{
        private View rootView;
        public CreateItemViewHolder(View view){
            super(view);
            rootView = view.findViewById(R.id.rootview_item_create_recyclerview_activity_moveselect);
        }
    }
    private class ItemDecoration extends RecyclerView.ItemDecoration{
        private Paint paint = new Paint();
        private int paintColor;
        private Path mPath;
        public ItemDecoration(){
            paintColor = getResources().getColor(R.color.darkgrey);
        }
        @Override
        public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state){
        }
        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);
            //c.drawColor(paintColor);
        }
        private void initPath(){

        }
        @Override
        public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDrawOver(c, parent, state);
        }
    }
}
