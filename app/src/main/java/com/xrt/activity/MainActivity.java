package com.xrt.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.xrt.R;
import com.xrt.authority.AuthorityController;
import com.xrt.authority.AuthorityCtrlFactory;
import com.xrt.constant.StringConstant;
import com.xrt.accesser.appinfo.AppInfoAccesser;
import com.xrt.accesser.appinfo.AppInfoAccesserFactory;
import com.xrt.accesser.originpic.OriginPicAccesserFactory;
import com.xrt.accesser.originpic.OriginPicAccesser;
import com.xrt.accesser.scanpic.ScanPicAccesser;
import com.xrt.accesser.scanpic.ScanPicAccesserFactory;
import com.xrt.thirdpartylib.itext.PdfUtils;
import com.xrt.tools.IOUtils;
import com.xrt.constant.DimenInfo;
import com.xrt.constant.CommonExtraTag;
import com.xrt.tools.ShareTools;
import com.xrt.tools.UiTools;
import com.xrt.tools.Utils;
import com.xrt.widget.HorizonImageTextButtonGroup;
import com.xrt.widget.VerticalImageTextButtonGroup;
import com.xrt.widget.WindowPoper;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

//首页
public class MainActivity extends AppCompatActivity {
    private ArrayList<String> mScanedFileDirs = new ArrayList<>();//目录格式为"scan_xxxxx"
    private ArrayList<PicInfo> mPreviewPicInfos = new ArrayList<>();
    private ArrayList<String> mNeedToScanFileNames = new ArrayList<>();
    private String mExternalFilePath;
    private String mPathSeperator;
    private RecyclerViewAdapter mAdapter;
    private Thread mFisrtLoadThread;
    private ChangeBounds mChangeBounds;
    private List<Integer> mSelectedItemPosList = new ArrayList<>();
    private DisplayMetrics mDisplayMetrics;
    private DisplayMetrics mRealDisplayMetrics;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mPreferencesEditor;
    private OriginPicAccesser mOriginPicAccesser;
    private ScanPicAccesser mScanPicAccesser;
    private AuthorityController mAuthorityCtr;
    private int mClickedItemPosition;
    private int mLastClickedItemPosition = -1;
    private int mOptionState = OPTION_DO_NOTHING;

    //StartActivityForResult方法启动其他Activity的常量
    public static final int CONNECT_PICVIEW_ACTIVITY = 0x10;//启动PicViewActivity的requestCode
    public static final int CONNECT_FILE_EXPLORER = 0x11;//调用文件浏览器
    public static final int CONNECT_FLATVIEW_ACTIVITY = 0x12;
    public static final int CONNECT_IMGSELECT_ACTIVITY = 0X13;
    public static final int OPTION_DO_NOTHING = 1;
    public static final int OPTION_DELETE_ITEM = 2;
    public static final int OPTION_IMPORT_PDF = 3;
    public static final int OPTION_IMPORT_IMG = 4;
    public static final int FORCE_UPDATE_PREVIEW = 5;
    private boolean isForceUpdatePreview = false;
    //页面状态常量
    private static final int STATE_CLICK = 1;
    private static final int STATE_CLICK_CANCEL = 2;
    private static final int STATE_ENTER_SELECTMODE = 3;
    private static final int STATE_QUIT_SELECTMODE = 4;
    private static final int STATE_SELECTALL = 5;
    private static final int STATE_SELECTALL_CANCEL = 6;
    private static final int STATE_UPDATE_POSITION = 7;
    private static final int STATE_RENAME = 8;
    private static final int STATE_JUST_NOW = 9;
    private static final int STATE_JUST_NOW_RESET = 10;
    //其他常量
    public static final String SCAM_TAG = "scan_";
    private static final String PDF_TAG = "pdf_";
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";
    private static final int PREVIEW_PIC_INSAMPLE_SIZE = 4;
    private static final String LAST_MODIFIED_TIME = "last_modified_time";
    private static final String PREFERENCE_DATA_URL = "/data/data";
    private static final String PREFERENCE_FILE_URL = "shared_prefs";
    //权限相关
    private static final int REQUEST_AUTHORITIES = 0x10;

    //view fields
    private ConstraintLayout mRootView;
    private TextView mMaskView;
    private ImageView mNoFileImageView;
    private TextView mNoFileTextView;
    private Button mFab;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ConstraintLayout mToolbar;
    private ConstraintLayout mSelectbar;
    private ConstraintLayout mFootbar;
    private TextView mSelectbarTextView;
    private Button mSelectAllButton;
    private HorizonImageTextButtonGroup mButtonGroup;
    private HorizonImageTextButtonGroup.ImageTextButton mDeleteButton;
    private Button mToolbarMenuButton;
    private PopupMenu mToolbarPopupMenu;
    private Button mSelectbarCloseButton;
    private ConstraintLayout mSingleItemMenuPopupWindowRootview;
    private View mRenamePopupWindowRootview;
    private TextView mRenamePopupWindowConfirmButton;
    private TextView mRenamePopupWindowCancelButton;
    private Button mRenamePopupWindowClearButton;
    private EditText mRenameEditText;
    private PopupWindow mRenamePopupWindow;
    private PopupWindow mSingleItemMenuPopupWindow;
    private TextView mPaddingTextView;
    private TextView mSingleItemMenuTitle;
    private TextView mSingleItemMenuFileSize;
    private HorizonImageTextButtonGroup mPopupWindowFenXingButtomGroup;
    private HorizonImageTextButtonGroup.ImageTextButton mWXFenxiang;
    private HorizonImageTextButtonGroup.ImageTextButton mQQFenxiang;
    private HorizonImageTextButtonGroup.ImageTextButton mMoreFenxiang;
    private VerticalImageTextButtonGroup mPopupWindowSubmenuButtomGroup;
    private VerticalImageTextButtonGroup.ImageTextButton mSingleItemMenuRenameButton;
    private VerticalImageTextButtonGroup.ImageTextButton mSingleItemMenuDeleteButton;
    private View mLoadingPdfHintRootView;
    private PopupWindow mLoadingPdfHintPopWindow;
    private PopupWindow mImportingHintPopWindow;
    private View mImportingHintPopWindowRootView;
    private PopupWindow mDeleteHintPopwindow;
    private View mDeleteHintPopwindowRootView;
    private WindowPoper mWindowPoper;
    private AppInfoAccesser mAppInfoAccesser;
    //flags
    private boolean isClickItemToPicViewActivity = false;
    private boolean mIsRecyclerLoaded = false;
    private boolean isSelectMode = false;
    private boolean isSingleItemMenuMode = false;
    private boolean isSelectAll = false;
    //size
    private int mToolbarHeight;//长按后才会获得值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initNormalVar();
        initAccessers();
        initView();
        initFootbarButtonGroup();
        initSingleItemMenuFenxiangButtonGroup();
        initSingleItemSubMenuButtonGroup();
        initAnthorityReqPopWindow();
        requestAuthoritiesWhenFirstStartApp();
        initFirstLoadThread();
        initRecyclerView();
        setViewsClickListener();
    }
    private void initFirstLoadThread(){
        mFisrtLoadThread =  new Thread(() -> {
            mIsRecyclerLoaded = true;
            scanPictureFileNames();
            addFiltedFileName(mNeedToScanFileNames, false);
        });
    }
    private void initRecyclerView(){
        mRecyclerView = findViewById(R.id.recycleview_activity_main);
        mRecyclerView.setHasFixedSize(true);
        //RecyclerView设置Adapter
        mAdapter = new RecyclerViewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        //RecyclerView设置LayoutManager
        mLinearLayoutManager = new CustomLinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        //RecyclerView设置ItemDecoration
        RecyclerItemDecoration itemDecoration = new RecyclerItemDecoration();
        mRecyclerView.addItemDecoration(itemDecoration);
        //RecyclerView设置ItemTouchHelper
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.post(() -> {
            mFisrtLoadThread.start();
        });
    }
    private void requestAuthoritiesWhenFirstStartApp(){
        if (!mAppInfoAccesser.isAuthoritiesRequested()){
            mWindowPoper.post(() -> {mWindowPoper.pop(Gravity.BOTTOM, true);});
        }
    }
    private void initAccessers(){
        mAppInfoAccesser = AppInfoAccesserFactory.createAppInfoAccesser(this);
        mOriginPicAccesser = OriginPicAccesserFactory.createOriginImgAccesser(this);
    }
    /*
     * 初始化一般变量放在这
     */
    private void initNormalVar(){
        mChangeBounds = new ChangeBounds();
        mChangeBounds.setDuration(100);
        mPathSeperator = "/";
        mPreferences = getSharedPreferences("scancam", Context.MODE_PRIVATE);
        mPreferencesEditor = mPreferences.edit();
        mDisplayMetrics = UiTools.getScreenMetrics(this);
        mRealDisplayMetrics = UiTools.getRealScreenMetrics(this);
        mScanPicAccesser = ScanPicAccesserFactory.createScanPicAccesser(this);
        mAuthorityCtr = AuthorityCtrlFactory.createAuthorityCtrl(this);
    }
    /**
     * @param position item对应的position
     * 返回Item对应的储存图片的目录的完整路径。
     */
    private String getItemPicDirAbsPath(int position){
        return getExternalFilesDir(mScanedFileDirs.get(position)).getPath();
    }
    /**
     * @param position item对应的position
     * 返回Item对应的储存图片的目录名。格式为"scam_xxxxxx" xxxx为拍摄时生成的时间戳。
     */
    private String getItemPicDirName(int position){
        return mScanedFileDirs.get(position);
    }
    /**
     * @param position 指定Item
     * 返回Item对应的pdf文件存储目录名。
     */
    private String getItemPdfDirName(int position){
        String itemPicDirName = getItemPicDirName(position);
        return itemPicDirName.replace(SCAM_TAG, PDF_TAG);
    }
    /**
    /**
     * @param position 指定Item
     * 返回pdf文件所在储存目录的完整路径。
     */
    private String getItemPdfAbsStoreDirPath(int position){
        String itemPdfDirName = getItemPdfDirName(position);
        return getExternalFilesDir("pdf" + mPathSeperator + getItemPdfDirName(position)).getPath();
    }
    /**
     * @param position 指定Item
     * @param specName 自己指定pdf的文件名。传入null使用默认文件名。
     * @param isAttachSurfix 是否带扩展名。
     * 返回Item对应的pdf文件的文件名。
     */
    private String getItemPdfFileName(int position, String specName, boolean isAttachSurfix){
        String pdfFileName;
        if (specName != null){
            pdfFileName = specName;
        }else{
            pdfFileName = getItemTitle(position, TIME_FORMAT);
        }
        return isAttachSurfix ? pdfFileName + ".pdf" : pdfFileName;
    }
    /**
     * @param position 指定Item
     * 返回Item对应的pdf文件的完整储存路径。
     */
    private String getItemPdfAbsFilePath(int position){
        String itemPdfAbsStoreDirPath = getItemPdfAbsStoreDirPath(position);
        String pdfName = getItemPdfFileName(position, null, true);
        return itemPdfAbsStoreDirPath + mPathSeperator + pdfName;
    }
    /**
     * @param position 指定Item
     * 判断Item对应的图片是否有变化
     */
    private boolean isPicChange(int position){
        String itemPicDirAbsPath = getItemPicDirAbsPath(position);
        long lastModifiedTime = new File(itemPicDirAbsPath).lastModified();
        SharedPreferences itemPreference = getItemPreferences(position);
        long cacheLastModifiedTime = itemPreference.getLong(LAST_MODIFIED_TIME, 0);
        //Log.d("mxrt", "newlastTime:" + lastModifiedTime);
        //Log.d("mxrt", "cachelastTime:" + cacheLastModifiedTime);
        boolean isChange = lastModifiedTime != cacheLastModifiedTime;
        return isChange;
    }
    /**
     * @param position 图片所在的文件夹的完整路径
     * @param pdfOutputPath 存放生成的pdf的完整路径
     * 将Item对应的所有jpg图片转成pdf文件输出至指定路径
     */
    private void ImgToPdf(int position, String pdfOutputPath){
        List<String> imgFilePaths = getItemPicAbsPaths(position);
        Document document = new Document();
        File outputFile = new File(pdfOutputPath);
        try(FileOutputStream outputStream = new FileOutputStream(outputFile)){
            PdfWriter.getInstance(document, outputStream);
            document.open();
            for (int i = 0; i < imgFilePaths.size(); i++){
                String imgPath = imgFilePaths.get(i);
                Bitmap pic = BitmapFactory.decodeFile(imgPath);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                pic.compress(Bitmap.CompressFormat.JPEG, 20, stream);
                Image image = Image.getInstance(stream.toByteArray());
                float widthScaleRate = ((document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin()) / image.getWidth()) * 100;
                float heightScaleRate = ((document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin()) / image.getHeight()) * 100;
                float scaleRate = Math.min(widthScaleRate, heightScaleRate);
                image.scalePercent(scaleRate);
                image.setAlignment(Image.ALIGN_CENTER);
                document.add(image);
            }
            document.close();
            SharedPreferences.Editor itemPreferenceEditor = getItemPreferences(position).edit();
            itemPreferenceEditor.putLong(LAST_MODIFIED_TIME, (new File(getItemPicDirAbsPath(position))).lastModified());
            itemPreferenceEditor.commit();
        }
        catch(Exception e){
            e.printStackTrace();
            //Log.d("mxrt", "" + e.getMessage());
         }
    }
    /**
     * @param position 指定Item
     * @param shareChannel 指定分享渠道
     * 将对应的Item生成pdf文件并分享
     */
    private void generateAndShareItemPdf(int position, int shareChannel){
        String pdfPath = getItemPdfAbsFilePath(position);
        mLoadingPdfHintPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        Thread thread = new Thread(() -> {
            //TestTool.printStringArray("mxrt", getItemPicAbsPaths(position).toArray());
            PdfUtils.imgToPdf(getItemPicAbsPaths(position), pdfPath, 20);
            mRootView.post(() -> {
                mLoadingPdfHintPopWindow.dismiss();
            });
            try{
                ShareTools.shareFile(pdfPath, "application/pdf", this, "com.xrt.fileprovider", shareChannel);
            }catch (Exception e){
                mRootView.post(() -> {
                    Toast.makeText(mRootView.getContext(), R.string.share_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
        thread.start();
    }
    /**
     * 设置View点击监听
     */
    private void setViewsClickListener(){
        mFab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startCameraActivity();
            }
        });
        mToolbarPopupMenu.inflate(R.menu.menu_toolbar_activity_main);
        mToolbarPopupMenu.setOnMenuItemClickListener((item) -> {
            switch(item.getItemId()){
                case R.id.option_selectall_menu_toolbar_activity_main:
                    enterSelectInterface();
                    break;
                case R.id.option_import_pdf_menu_toolbar_activity_main:
                    openPdfFileExplore();
                    break;
                case R.id.option_import_pic_menu_toolbar_activity_main:
                    openPicFileExplore();
                    break;
            }
            return true;
        });
        mToolbarMenuButton.setOnClickListener((view) -> {
            mToolbarPopupMenu.show();
        });
        mSelectbarCloseButton.setOnClickListener((view) -> {
            quitSelectInterface();
            mSelectedItemPosList.clear();
        });
        mPaddingTextView.setOnClickListener((view) -> {
            quitSingleItemMenu();
        });
        OpenCVLoader.initDebug();
    }
    private void initAnthorityReqPopWindow(){
        mWindowPoper = new WindowPoper(mRootView, R.layout.popwindow_request_authority_hint, mDisplayMetrics.widthPixels, (int)(mDisplayMetrics.heightPixels * 0.3f));
        mWindowPoper.setCustomizer((rootView) -> {
                Button okButton = rootView.findViewById(R.id.button_confirm_req_authority_popwindow);
                TextView content = rootView.findViewById(R.id.title_req_authority_popwindow);
                content.setText(R.string.req_authority_main_activity);
                okButton.setOnClickListener((v) -> {
                    mWindowPoper.dismiss();
                });
            }
        );
        mWindowPoper.setDismissListener(() -> {
            mAuthorityCtr.requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_AUTHORITIES);
            mAppInfoAccesser.authoritiesRequestCompleted();
        });
    }
    private void pdfToImgs(Uri uri, String picStorePath)
    throws Exception
    {
        ParcelFileDescriptor pd = null;
        PdfRenderer renderer = null;
        PdfRenderer.Page page;
        try{
            //pd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pd = getContentResolver().openFileDescriptor(uri, "r");
            Log.d("mxrt", "fd:" + pd.getFd());
            renderer = new PdfRenderer(pd);
            for (int i = 0; i < renderer.getPageCount(); i++){
                page = renderer.openPage(i);
                float scaleFactorX = mDisplayMetrics.ydpi / 72;
                int width = (int)(page.getWidth() * scaleFactorX);
                int height = (int)(page.getHeight() * scaleFactorX);
                Bitmap pic = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                pic.eraseColor(Color.parseColor("#FFFFFF"));
                page.render(pic, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                String picPath = picStorePath + File.separator + System.currentTimeMillis() + ".jpg";
                IOUtils.saveImgFileWithJpg(picPath, pic, 20);
                if (page != null) page.close();
            }
        }catch(Exception e){
            throw new Exception();
        }finally {
            if (renderer != null) {renderer.close(); Log.d("mxrt", "renderer closed");}
            try{if (pd != null) pd.close(); Log.d("mxrt", "pd closed");} catch (Exception e){ e.printStackTrace(); }
        }
    }
    private void openPdfFileExplore(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, CONNECT_FILE_EXPLORER);
    }
    private void openPicFileExplore(){
        Intent intent = new Intent(CommonExtraTag.START_IMGSELECT_ACTIVITY_ACTION);
        startActivityForResult(intent, CONNECT_IMGSELECT_ACTIVITY);
    }
    /**
     * 如果首页没有文档，弹出的提示图标的初始化。
     */
    private void initNoFileReminder(){
        if (mScanedFileDirs.size() != 0){
            mNoFileImageView.setVisibility(View.GONE);
            mNoFileTextView.setVisibility(View.GONE);
        }else{
            mNoFileImageView.setVisibility(View.VISIBLE);
            mNoFileTextView.setVisibility(View.VISIBLE);
        }
    }
    /**
     * 多选模式界面的底部菜单按钮的初始化。
     */
    private void initFootbarButtonGroup(){
        mButtonGroup.addImageTextButtom(3);
        mButtonGroup.setWidthMarginPercent(0.1f);
        mDeleteButton = mButtonGroup.getButtonAt(2);
        mDeleteButton.getImageView().setImageResource(R.drawable.delete_button);
        mDeleteButton.getTextView().setText("删除");
        mDeleteButton.getTextView().setTextColor(Color.BLACK);
        mDeleteButton.setOnClickListener((view) -> {
            if (mSelectedItemPosList.size() == 0){
                Toast toast = Toast.makeText(this, R.string.no_select_remind, Toast.LENGTH_SHORT);
                toast.show();
            }else{
                popDeleteHintPopWindow(mMaskView,
                        (v) -> {
                            quitSelectInterface();
                            deleteItems(mSelectedItemPosList);
                            mSelectedItemPosList.clear();
                            mDeleteHintPopwindow.dismiss();},
                        (v) -> {mDeleteHintPopwindow.dismiss(); });
            }
        });
    }
    /**
     * Item长按菜单中分享按钮的初始化。
     */
    private void initSingleItemMenuFenxiangButtonGroup(){
        mPopupWindowFenXingButtomGroup.addImageTextButtom(3);
        mWXFenxiang = mPopupWindowFenXingButtomGroup.getButtonAt(0);
        mWXFenxiang.getImageView().setImageResource(R.drawable.weixin);
        mWXFenxiang.getTextView().setText("微信");

        mQQFenxiang = mPopupWindowFenXingButtomGroup.getButtonAt(1);
        mQQFenxiang.getImageView().setImageResource(R.drawable.qq_share_button);
        mQQFenxiang.getTextView().setText("QQ");

        mMoreFenxiang = mPopupWindowFenXingButtomGroup.getButtonAt(2);
        mMoreFenxiang.getImageView().setImageResource(R.drawable.more_button);
        mMoreFenxiang.getTextView().setText("更多");
    }
    /**
     * 根据对应Item更新分享按钮的点击响应
     */
    private void updateShareButtonClickListener(int position) {
        mWXFenxiang.setOnClickListener((view) -> {
            generateAndShareItemPdf(position, ShareTools.SHARE_WEIXIN);
            quitSingleItemMenu();
        });
        mQQFenxiang.setOnClickListener((view) -> {
            generateAndShareItemPdf(position, ShareTools.SHARE_QQ);
            quitSingleItemMenu();
        });
        mMoreFenxiang.setOnClickListener((view) -> {
            generateAndShareItemPdf(position, ShareTools.SHARE_MORE);
            quitSingleItemMenu();
        });
    }
    /**
     * Item长按菜单中下方的子菜单按钮的初始化。
     */
    private void initSingleItemSubMenuButtonGroup(){
        mPopupWindowSubmenuButtomGroup.addImageTextButton(4);
        mPopupWindowSubmenuButtomGroup.setAllImageViewGone();
        mPopupWindowSubmenuButtomGroup.setAllTextViewConstraintHeightPercent(1f);
        mSingleItemMenuRenameButton = mPopupWindowSubmenuButtomGroup.getButtonAt(0);
        mSingleItemMenuRenameButton.getTextView().setText("重命名");
        mSingleItemMenuRenameButton.getTextView().setTextSize(16);
        mSingleItemMenuRenameButton.getTextView().setTextColor(Color.parseColor("#BB000000"));
        mSingleItemMenuRenameButton.setBackgroundResource(R.drawable.button_normal_click_effect);
        mSingleItemMenuDeleteButton = mPopupWindowSubmenuButtomGroup.getButtonAt(1);
        mSingleItemMenuDeleteButton.getTextView().setText("删除");
        mSingleItemMenuDeleteButton.getTextView().setTextSize(16);
        mSingleItemMenuDeleteButton.getTextView().setTextColor(Color.parseColor("#BB000000"));
        mSingleItemMenuDeleteButton.setBackgroundResource(R.drawable.button_normal_click_effect);
    }
    /**
     * 页面View的初始化。
     */
    private void initView() {
        mMaskView = findViewById(R.id.mask_activity_main);
        mRootView = findViewById(R.id.rootview_activity_main);
        mNoFileImageView = findViewById(R.id.imageview_nofile_activity_main);
        mNoFileTextView = findViewById(R.id.textview_nofile_activity_main);
        mFab = findViewById(R.id.fab_activity_main);
        mRecyclerView = findViewById(R.id.recycleview_activity_main);
        mToolbar = findViewById(R.id.rootview_toolbar_activity_main);
        mSelectbar = findViewById(R.id.rootview_selectbar_activity_main);
        mSelectbarTextView = findViewById(R.id.textview_selectbar_activity_main);
        mSelectAllButton = findViewById(R.id.button_selectall_selectbar_activity_main);
        mFootbar = findViewById(R.id.footbar_activity_main);
        mButtonGroup = mFootbar.findViewById(R.id.buttongroup_footbar_activity_main);
        mToolbarMenuButton = findViewById(R.id.button_menu_toolbar_activity_main);
        mToolbarPopupMenu = new PopupMenu(this, mToolbarMenuButton);
        mSelectbarCloseButton = findViewById(R.id.button_close_selectbar_activity_main);
        mSingleItemMenuPopupWindowRootview = (ConstraintLayout)getLayoutInflater().inflate(R.layout.popwindow_singleitem_menu_activity_main, null);
        mPaddingTextView = mSingleItemMenuPopupWindowRootview.findViewById(R.id.textview_padding_popwindow_singleitem_menu_activity_main);
        mSingleItemMenuTitle = mSingleItemMenuPopupWindowRootview.findViewById(R.id.textview_title_popupwindow_singleitem_menu_activity_main);
        mSingleItemMenuFileSize = mSingleItemMenuPopupWindowRootview.findViewById(R.id.textview_size_popupwindow_singleitem_menu_activity_main);
        mPopupWindowFenXingButtomGroup = mSingleItemMenuPopupWindowRootview.findViewById(R.id.buttongroup_popupwindow_singleitem_menu_activit_main);
        mPopupWindowSubmenuButtomGroup = mSingleItemMenuPopupWindowRootview.findViewById(R.id.buttongroup_submenu_popwindow_singleitem_menu_activity_main);
        mRenamePopupWindowRootview = getLayoutInflater().inflate(R.layout.popwindow_rename_activity_main, null);
        mRenamePopupWindowConfirmButton = mRenamePopupWindowRootview.findViewById(R.id.confirm_button_rename_popwindow_activity_main);
        mRenamePopupWindowCancelButton = mRenamePopupWindowRootview.findViewById(R.id.cancel_button_rename_popwindow_activity_main);
        mRenamePopupWindowClearButton = mRenamePopupWindowRootview.findViewById(R.id.button_clear_rename_popwindow_activity_main);
        mRenameEditText = mRenamePopupWindowRootview.findViewById(R.id.edittext_rename_popwindow_activity_main);
        //设置rename_popwindow
        int renamePopWindowWidth = (int)(mDisplayMetrics.widthPixels * DimenInfo.RENAME_POPWINDOW_WIDTH_PECENT);
        int renamePopWindowHeight = UiTools.dpTopx(this, DimenInfo.RENAME_POPWINDOW_HEIGHT);
        mRenamePopupWindow = new PopupWindow(mRenamePopupWindowRootview, renamePopWindowWidth, renamePopWindowHeight);
        mLoadingPdfHintRootView = getLayoutInflater().inflate(R.layout.popwindow_loadingpdf_hint, null);
        mLoadingPdfHintPopWindow = new PopupWindow(mLoadingPdfHintRootView, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        mImportingHintPopWindowRootView = getLayoutInflater().inflate(R.layout.popwindow_importingpdf_hint, null);
        mImportingHintPopWindow = new PopupWindow(mImportingHintPopWindowRootView, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        mDeleteHintPopwindowRootView = getLayoutInflater().inflate(R.layout.popwindow_delete_hint, null);
        int deleteHintPopwindowHeight = getResources().getDimensionPixelSize(R.dimen.popwindow_delete_hint_height);
        mDeleteHintPopwindow = new PopupWindow(mDeleteHintPopwindowRootView, mDisplayMetrics.widthPixels, deleteHintPopwindowHeight);
    }
    private void startAuthoritiesSetting(){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(StringConstant.PACKAGE_URI_SCHEME + getPackageName()));
        startActivity(intent);
    }
    /**
     * Item长按菜单弹窗的初始化。
     */
    private void initSingleItemMenuWindow(){
        DisplayMetrics metrics = UiTools.getRealScreenMetrics(this);
        int statusBarHeight = UiTools.getStatusBarHeight(this);
        int navBarHeight = UiTools.getNavBarHeight(this);
        int windowHeight = metrics.heightPixels - statusBarHeight - navBarHeight;
        mSingleItemMenuPopupWindow = new PopupWindow(mSingleItemMenuPopupWindowRootview, mDisplayMetrics.widthPixels, windowHeight);
    }
    /**
     * 启动CameraActivity。
     */
    private void startCameraActivity(){
        Intent intent = new Intent(CommonExtraTag.START_CAMERA_ACTIVITY_ACTION);
        intent.putExtra(CommonExtraTag.CAMERA_ACTIVITY_TYPE, CameraActivity.TYPE_PREVIEW);
        intent.putExtra(CommonExtraTag.SOURCE_ACTIVITY, CameraActivity.START_FROM_MAIN_ACTIVITY);
        startActivity(intent);
    }
    /**
     * 启动PicViewAcitivy。
     */
    private void startPicviewActivity(int position){
        Intent intent = new Intent(CommonExtraTag.START_PICVIEW_ACTIVITY_ACTION);
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILEPATH_EXTRA, getExternalFilesDir(mScanedFileDirs.get(position)).getPath());
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_FILE_TITLE, getItemTitle(position, TIME_FORMAT));
        intent.putExtra(CommonExtraTag.PICVIEW_ACTIVITY_TYPE, PictureViewActivity.TYPE_START_FROM_ACTIVITY);
        intent.putExtra(CommonExtraTag.ITEM_POSITION, position);
        startActivityForResult(intent, CONNECT_PICVIEW_ACTIVITY);
    }
    private void startFlatViewActivity(int position){
        Intent intent = new Intent(CommonExtraTag.START_FLATVIEW_ACTIVITY_ACTION);
        intent.putExtra(CommonExtraTag.FLATVIEW_ACTIVITY_PICABSPATH, getExternalFilesDir(mScanedFileDirs.get(position)).getPath());
        intent.putExtra(CommonExtraTag.FLATVIEW_ACTIVITY_TITLE, getItemTitle(position, TIME_FORMAT));
        intent.putExtra(CommonExtraTag.FLATVIEW_ACTIVITY_PDFDIR_ABSPATH, getItemPdfAbsStoreDirPath(position));
        intent.putExtra(CommonExtraTag.FLATVIEW_ACTIVITY_TYPE, FlatViewActivity.TYPE_START_FROM_ACTIVITY_MAIN);
        intent.putExtra(CommonExtraTag.ITEM_POSITION, position);
        startActivityForResult(intent, CONNECT_FLATVIEW_ACTIVITY);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == CONNECT_PICVIEW_ACTIVITY){
            if (resultCode == OPTION_DELETE_ITEM){
                mOptionState = OPTION_DELETE_ITEM;
                int position = intent.getIntExtra(CommonExtraTag.ITEM_POSITION, -1);
                if (position >= 0){
                    deleteOneItem(position);
                }
            }
        }else if (requestCode == CONNECT_FILE_EXPLORER){
            if (resultCode == AppCompatActivity.RESULT_OK){
                mOptionState = OPTION_IMPORT_PDF;
                Uri uri = intent.getData();
                Log.d("mxrt", "uri:" + uri);
                String picStoreDir = getExternalFilesDir("scan_" + System.currentTimeMillis()).getPath();
                Thread thread = new Thread(() -> {
                    try {
                        pdfToImgs(uri, picStoreDir);
                    }catch(Exception e){
                        e.printStackTrace();
                        mRootView.post(() -> {
                            Toast.makeText(this, R.string.pdf_import_error, Toast.LENGTH_LONG).show();
                        });
                    }
                    mRootView.post(() -> {
                        mImportingHintPopWindow.dismiss();
                        scanNewPicDir();
                    });
                });
                thread.start();
                mRootView.post(() -> {
                    mImportingHintPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);});
            }
        }else if (requestCode == CONNECT_IMGSELECT_ACTIVITY){
            if (resultCode == OPTION_IMPORT_IMG){
                mOptionState = OPTION_IMPORT_IMG;
                mImportingHintPopWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
                String imgStoreDir = getExternalFilesDir("scan_" + System.currentTimeMillis()).getPath();
                Bundle bundle = intent.getExtras();
                ArrayList<String> uriStrings = bundle.getStringArrayList(CommonExtraTag.IMGSELECT_ACTIVITY_URI_STRINGS);
                ArrayList<String> orientStrings = bundle.getStringArrayList(CommonExtraTag.IMGSELECT_ACTIVITY_ORIENTATION_STRINGS);
                Thread thread = new Thread(() -> {
                    storeImgs(imgStoreDir, uriStrings, orientStrings);
                    mRootView.post(() -> {
                        mImportingHintPopWindow.dismiss();
                        scanNewPicDir();
                    });
                });
                thread.start();
            }
        }else if (requestCode == CONNECT_FLATVIEW_ACTIVITY){
            if (resultCode == OPTION_DELETE_ITEM){
                mOptionState = OPTION_DELETE_ITEM;
                int position = intent.getIntExtra(CommonExtraTag.ITEM_POSITION, -1);
                if (position >= 0){
                    deleteOneItem(position);
                }
            }
            if (resultCode == FORCE_UPDATE_PREVIEW){
                isForceUpdatePreview = true;
            }
        }
    }
    private void storeImgs(String imgStoreDir, List<String> uriStrings, List<String> orientStrings){
        try{
            for (int i = 0; i < uriStrings.size(); i++){
                String uriString = uriStrings.get(i);
                String orient = orientStrings.get(i);
                Uri uri = Uri.parse(uriString);
                String picDirName = imgStoreDir.replace(getExternalFilesDir("").getPath() + File.separator, "");
                String picName = System.currentTimeMillis() + ".jpg";
                String imgStorePath = imgStoreDir + File.separator + picName ;
                ParcelFileDescriptor.AutoCloseInputStream stream = (ParcelFileDescriptor.AutoCloseInputStream)getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                Bitmap rotatedBitmap;
                if (orient != null && orient != "0"){
                    Matrix matrix = new Matrix();
                    matrix.postRotate(Float.valueOf(orient));
                    rotatedBitmap  = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),  matrix, true);
                }else{
                    rotatedBitmap = bitmap;
                }
                IOUtils.saveImgFileWithJpg(imgStorePath, rotatedBitmap, 20);
                storeImportedOriginPic(picDirName, picName, rotatedBitmap, 20);
                bitmap.recycle();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void storeImportedOriginPic(String scanDirName, String picName, Bitmap pic, int quanlity){
        String relativePath = File.separator + scanDirName + File.separator + picName;
        mOriginPicAccesser.storeOriginPic(relativePath, pic, quanlity);
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ItemViewHolder>{
        @Override
        @NonNull
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_recyclerview_activity_main, parent, false);
            //ViewGroup.LayoutParams rootViewLP = view.getLayoutParams();
            //rootViewLP.width = parent.getWidth();
            return new ItemViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position){
        }
        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position, List<Object> payLoads){
            if (payLoads.isEmpty()){
                //Log.d("mxrt", "entered");
                String title = getItemTitle(position, TIME_FORMAT);
                String timeString = getFileCreateTime(position, TIME_FORMAT);
                viewHolder.imageView.setImageBitmap(mPreviewPicInfos.get(position).bitmap);
                viewHolder.titleTextView.setText(title);
                viewHolder.timeTextView.setText(timeString);
                if (isSelectMode){
                    viewHolder.checkView.setBackgroundResource(R.drawable.button_item_check_activity_main_unpress_bg);
                    viewHolder.checkView.setVisibility(View.VISIBLE);
                    //渲染回checkView已经设置的状态
                    if (mSelectedItemPosList.contains(position)){
                        viewHolder.checkView.setBackgroundResource(R.drawable.button_item_check_activity_main_press_bg);
                    }
                }else{
                    viewHolder.checkView.setVisibility(View.INVISIBLE);
                }
            }else{
                for (int i = 0; i < payLoads.size(); i++){
                    int flags = (int)payLoads.get(i);
                    switch (flags){
                        case STATE_CLICK:
                        case STATE_SELECTALL:
                            viewHolder.checkView.setBackgroundResource(R.drawable.button_item_check_activity_main_press_bg);
                            break;
                        case STATE_ENTER_SELECTMODE:
                        case STATE_CLICK_CANCEL:
                        case STATE_SELECTALL_CANCEL:
                            viewHolder.checkView.setBackgroundResource(R.drawable.button_item_check_activity_main_unpress_bg);
                            viewHolder.checkView.setVisibility(View.VISIBLE);
                            break;
                        case STATE_QUIT_SELECTMODE:
                            viewHolder.checkView.setVisibility(View.INVISIBLE);
                            break;
                        case STATE_UPDATE_POSITION:
                            break;
                        case STATE_RENAME:
                            viewHolder.titleTextView.setText(getItemTitle(position, TIME_FORMAT));
                            break;
                        case STATE_JUST_NOW:
                            viewHolder.timeTextView.setText(getFileCreateTime(position, TIME_FORMAT) + "  刚刚打开");
                            mLastClickedItemPosition = mClickedItemPosition;
                            break;
                        case STATE_JUST_NOW_RESET:
                            viewHolder.timeTextView.setText(getFileCreateTime(mLastClickedItemPosition, TIME_FORMAT));
                            break;
                    }
                }
            }
            updateViewOnClickListener(viewHolder.rootView, viewHolder.getAdapterPosition());
            updateViewOnLongClickListener(viewHolder.rootView, viewHolder, viewHolder.getAdapterPosition());
        }
        @Override
        public int getItemCount(){
            initNoFileReminder();
            Log.d("mxrt", "getcount");
            return mScanedFileDirs.size();
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder{
        View rootView;
        ImageView imageView;
        TextView titleTextView;
        TextView timeTextView;
        TextView checkView;


        public ItemViewHolder(View view){
            super(view);
            rootView = view.findViewById(R.id.rootview_item_recyclerview_activity_main);
            imageView = view.findViewById(R.id.imageview_item_recyclerview_activity_main);
            titleTextView = view.findViewById(R.id.textview_title_item_recyclerview_activity_main);
            timeTextView = view.findViewById(R.id.textview_time_item_recyclerview_activity_main);
            checkView = view.findViewById(R.id.button_item_check_activity_main);
        }
    }

    public class ItemTouchHelperCallback extends ItemTouchHelper.Callback{
        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder){
            //int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int dragFlags = 0;
            int swipeFlags = 0;
            int flags = makeMovementFlags(dragFlags, swipeFlags);
            return flags;
        }
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder targetViewHolder){
            int fromPos = viewHolder.getAdapterPosition();
            int toPos = targetViewHolder.getAdapterPosition();
            Collections.swap(mScanedFileDirs, fromPos, toPos);
            updateViewOnClickListener(((ItemViewHolder)viewHolder).rootView, toPos);
            updateViewOnClickListener(((ItemViewHolder)targetViewHolder).rootView, fromPos);
            mAdapter.notifyItemMoved(fromPos, toPos);
            return true;
        }
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction){
            deleteOneItem(viewHolder.getAdapterPosition());
        }
        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dx, float dy, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
        }
    }
    public class RecyclerItemDecoration extends RecyclerView.ItemDecoration{
        private static final float GAP_RATE_OF_ITEM_HEIGHT = 0.012f;
        private static final float GAP_RATE_OF_ITEM_WIDTH = 0.03f;

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state){
            super.getItemOffsets(outRect, view, parent, state);
            DisplayMetrics metrics = UiTools.getScreenMetrics(parent.getContext());
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;
            outRect.bottom = (int)(GAP_RATE_OF_ITEM_HEIGHT * screenHeight);
            outRect.right = (int)(GAP_RATE_OF_ITEM_WIDTH * screenWidth);
            outRect.left = (int)(GAP_RATE_OF_ITEM_WIDTH * screenWidth);
        }
        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state){
        }
    }
    public class CustomLinearLayoutManager extends LinearLayoutManager {

        public CustomLinearLayoutManager(Context context){
            super(context);
        }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state){
            super.onLayoutChildren(recycler, state);
        }
        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state){
            return super.scrollVerticallyBy(dy, recycler, state);
        }
    }
    /**
     * @param position 指定对应的Item项
     * @param format 指定生成的时间字符串的格式
     * 解析出Item对应的创建时间字符串。
     */
    private String getFileCreateTime(int position, String format){
        String fileName = mScanedFileDirs.get(position);
        String timeString = fileName.replace(SCAM_TAG, "");
        long timeStamp = Long.parseLong(timeString);
        return Utils.timestampToFormat(timeStamp, format);
    }
    /**
     * 更新Item的单击响应。
     */
    private void updateViewOnClickListener(View view, int position){
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSelectMode){
                    selectItem(position);
                }else{
                    isClickItemToPicViewActivity = true;
                    mClickedItemPosition = position;
                    //startPicviewActivity(position);
                    startFlatViewActivity(position);
                }
            }
        });
    }
    /**
     * 更新Item的长按响应。
     */
    private void updateViewOnLongClickListener(View view, ItemViewHolder viewHolder, int position){
        view.setOnLongClickListener((v) -> {
            if (!isSingleItemMenuMode && !isSelectMode){
                enterSingleItemMenu(viewHolder, position);
                updateShareButtonClickListener(position);
            }
            return true;
        });
    }
    /**
     * 收展顶部菜单栏。
     */
    private void changeToolbarState(boolean isRestore){
        ViewGroup.LayoutParams mToolbarLP = mToolbar.getLayoutParams();
        if (isRestore){
            mToolbarLP.height = mToolbarHeight;
        }else{
            mToolbarLP.height = 0;
        }
        mToolbar.setLayoutParams(mToolbarLP);
    }
    /**
     * 收展多选模式的顶部菜单栏。
     */
    private void changeSelectbarState(boolean isRestore){
        ConstraintSet cs = new ConstraintSet();
        cs.clone(mRootView);
        ViewGroup.LayoutParams selectbarLP = mSelectbar.getLayoutParams();
        mToolbarHeight = mToolbar.getHeight();
        if (isRestore){
            cs.clear(mRecyclerView.getId(), ConstraintSet.TOP);
            cs.connect(mRecyclerView.getId(), ConstraintSet.TOP, mToolbar.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            selectbarLP.height = 0;
        }else{
            cs.clear(mRecyclerView.getId(), ConstraintSet.TOP);
            cs.connect(mRecyclerView.getId(), ConstraintSet.TOP, mSelectbar.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            selectbarLP.height = mToolbarHeight;
        }
        mSelectbar.setLayoutParams(selectbarLP);
    }
    /**
     * 收展Fab按钮。
     */
    private void changeFabState(boolean isRestore){
        ViewGroup.LayoutParams fabLP = mFab.getLayoutParams();
        if (isRestore){
            int desireWidth = UiTools.dpTopx(this, DimenInfo.FAB_WIDTH);
            fabLP.width = desireWidth;
            fabLP.height = desireWidth;
        }else{
            //mFabWidth = mFab.getWidth();
            //mFabHeight = mFab.getHeight();
            fabLP.width = 0;
            fabLP.height = 0;
        }
        mFab.setLayoutParams(fabLP);
    }
    /**
     * 收展多选模式的底部菜单栏。
     */
    private void changeFootbarState(boolean isRestore){
        ConstraintSet cs = new ConstraintSet();
        cs.clone(mRootView);
        ViewGroup.LayoutParams footbarLP = mFootbar.getLayoutParams();
        if (isRestore){
            cs.clear(mRecyclerView.getId(), ConstraintSet.BOTTOM);
            cs.connect(mRecyclerView.getId(), ConstraintSet.BOTTOM, mRootView.getId(), ConstraintSet.BOTTOM);
            cs.applyTo(mRootView);
            footbarLP.height = 0;
        }else{
            cs.clear(mRecyclerView.getId(), ConstraintSet.BOTTOM);
            cs.connect(mRecyclerView.getId(), ConstraintSet.BOTTOM, mFootbar.getId(), ConstraintSet.TOP);
            cs.applyTo(mRootView);
            int desireHeight = (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DimenInfo.FOOTBAR_DESIRE_HEIGHT, mFootbar.getContext().getResources().getDisplayMetrics()));
            footbarLP.height = desireHeight;
        }
    }
    /**
     * 收展Item项中的check标记。
     */
    private void changeCheckButtonState(boolean isRestore){
        if (isRestore){
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), STATE_QUIT_SELECTMODE);
        }else{
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), STATE_ENTER_SELECTMODE);
        }
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
    /**
     * @param viewHolder Item对应的viewHolder
     * @param position 指定对应的Item
     * 弹出Item的长按菜单。
     */
    private void popSingleItemMenuWindow(ItemViewHolder viewHolder, int position){
        mSingleItemMenuTitle.setText(viewHolder.titleTextView.getText());
        mSingleItemMenuFileSize.setText(String.format("大小：%.2f MB", IOUtils.getSubFileTotalSize(getItemPicDirAbsPath(position), IOUtils.FILE_SIZE_UNIT_MB,true)));
        mSingleItemMenuDeleteButton.setOnClickListener((view) -> {
            popDeleteHintPopWindow(mMaskView,
                    (v) -> {
                        deleteOneItem(position);
                        mDeleteHintPopwindow.dismiss();},
                    (v) -> {mDeleteHintPopwindow.dismiss();});
            quitSingleItemMenu();
        });
        //重命名按钮设置监听
        mSingleItemMenuRenameButton.setOnClickListener((view) -> {
            quitSingleItemMenu();
            changeFabState(false);
            mMaskView.setVisibility(View.VISIBLE);
            popRenameWindow(position);
            popRenameKeyBoard();
        });
        initSingleItemMenuWindow();
        mSingleItemMenuPopupWindow.showAtLocation(mRecyclerView, Gravity.CENTER, 0, 0);
    }
    /**
     * 收起Item的长按菜单弹窗。
     */
    private void dimissSingleItemMenuWindow(){
        mSingleItemMenuPopupWindow.dismiss();
    }
    /**
     * 收起重命名弹窗。
     */
    private void dimissRenameWindow(){
        mRenamePopupWindow.dismiss();
    }
    /**
     * 弹出重命名键盘。
     */
    private void popRenameKeyBoard(){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }
    /**
     * 弹出重命名弹窗
     */
    private void popRenameWindow(int position){
        mRenamePopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mMaskView.setVisibility(View.INVISIBLE);
                changeFabState(true);
                mRenameEditText.getText().clear();
            }
        });
        mRenamePopupWindowConfirmButton.setOnClickListener((view) -> {
            String inputTitle = mRenameEditText.getText().toString();
            mPreferencesEditor.putString(mScanedFileDirs.get(position), inputTitle);
            mPreferencesEditor.commit();
            mAdapter.notifyItemChanged(position, STATE_RENAME);
            dimissRenameWindow();
        });
        mRenamePopupWindowCancelButton.setOnClickListener((view) -> {
            dimissRenameWindow();
        });
        mRenamePopupWindowClearButton.setOnClickListener((view) -> {
            mRenameEditText.getText().clear();
        });
        mRenamePopupWindow.setFocusable(true);//这行不能少，否则重命名弹窗中的输入框无法获得焦点
        mRenamePopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mRenamePopupWindow.showAtLocation(mRootView, Gravity.CENTER, 0, 0);
        mRenameEditText.getText().insert(0, getItemTitle(position, TIME_FORMAT));
        mRenameEditText.requestFocus();
        //mRenameEditText.setFocusable(true);
    }
    /**
     * 为多选按钮添加点击响应
     */
    private void setSelectAllButtonClickListener(){
        mSelectAllButton.setOnClickListener((view) -> {
            if (isSelectAll){
                isSelectAll = false;
                mSelectAllButton.setBackgroundResource(R.drawable.selectall_button_black);
                mSelectedItemPosList.clear();
                mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), STATE_SELECTALL_CANCEL);
            }else{
                isSelectAll = true;
                mSelectAllButton.setBackgroundResource(R.drawable.selectall_button_blue);
                mSelectedItemPosList.clear();
                for (int i = 0; i < mAdapter.getItemCount(); i++){
                    mSelectedItemPosList.add(i);
                }
                mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount(), STATE_SELECTALL);
            }
        });
    }
    /**
     * @param positionList 指定需要删除的Item项
     * 删除给定的Item项
     */
    private void deleteItems(List<Integer> positionList){
        List<String> pathsNeedToDelete = new ArrayList<>();
        List<PicInfo> previewPicsNeedToDelete = new ArrayList<>();
        for (int position : positionList){
            mOriginPicAccesser.deleteOriginPicDir(File.separator + getItemPicDirName(position));
            String fileName = mScanedFileDirs.get(position);
            pathsNeedToDelete.add(mScanedFileDirs.get(position));
            previewPicsNeedToDelete.add(mPreviewPicInfos.get(position));
            String filePath = mExternalFilePath + "/" + fileName;
            IOUtils.deleteDirectoryOrFile(filePath);
            deleteItemPreference(position);
            deleteItemPdfFile(position);
            deleteItemPreferenceRecord(position);
            mAdapter.notifyItemRemoved(position);
            mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
        }
        mPreviewPicInfos.removeAll(previewPicsNeedToDelete);
        mScanedFileDirs.removeAll(pathsNeedToDelete);
    }
    /**
     * @param position 指定需要删除的Item项
     * 删除单个Item项
     */
    private void deleteOneItem(int position){
        //Log.d("mxrt", "deleteoneItem");
        Log.d("mxrt", "position:" + position);
        mOriginPicAccesser.deleteOriginPicDir(File.separator + mScanPicAccesser.getScanItemDirNames().get(position));
        Log.d("mxrt", "names:" + Arrays.toString(mScanPicAccesser.getScanItemDirNames().toArray()));
        Log.d("mxrt", "dir:" + mScanPicAccesser.getScanItemDirNames().get(position));
        deleteItemPreference(position);//要先删
        deleteItemPreferenceRecord(position);
        deleteItemPdfFile(position);//要先删
        mPreviewPicInfos.remove(position);
        String dirName = mScanedFileDirs.remove(position);
        String filePath = mExternalFilePath + mPathSeperator + dirName;
        IOUtils.deleteDirectoryOrFile(filePath);
        mAdapter.notifyItemRemoved(position);
        mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
    }
    /**
     * @param position 指定Item
     * 删除Item对应的SharePreferences文件
     */
    private void deleteItemPreference(int position){
        String preferencePath = PREFERENCE_DATA_URL + mPathSeperator + getPackageName() + mPathSeperator + PREFERENCE_FILE_URL + mPathSeperator + getItemPicDirName(position) + ".xml";
        boolean result = IOUtils.deleteDirectoryOrFile(preferencePath);
    }
    /**
     * @param position 指定Item
     * 删除item对应的pdf文件
     */
    private void deleteItemPdfFile(int position){
        String pdfPath = getItemPdfAbsStoreDirPath(position);
        IOUtils.deleteDirectoryOrFile(pdfPath);
    }
    /**
     * @param position 指定Item
     * 删除item在SharedPreferences的标题记录
     */
    private void deleteItemPreferenceRecord(int position){
        String itemDirName = getItemPicDirName(position);
        mPreferencesEditor.remove(itemDirName);
        mPreferencesEditor.apply();
    }
    /**
     * @param position 指定选择的Item项
     * 在多选模式下选择指定的Item
     */
    private void selectItem(int position){
        if (mSelectedItemPosList.contains(position)){
            mSelectedItemPosList.remove(Integer.valueOf(position));
            mAdapter.notifyItemChanged(position, STATE_CLICK_CANCEL);
            mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
        }else{
            mSelectedItemPosList.add(position);
            mAdapter.notifyItemChanged(position, STATE_CLICK);
            mSelectbarTextView.setText(String.format("已选择%d项", mSelectedItemPosList.size()));
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent){
        if (isSelectMode && keyCode == KeyEvent.KEYCODE_BACK){
            quitSelectInterface();
            mSelectedItemPosList.clear();
            return true;
        }
        if (isSingleItemMenuMode && keyCode == KeyEvent.KEYCODE_BACK){
            quitSingleItemMenu();
            return true;
        }
        return super.onKeyDown(keyCode, keyEvent);
    }
    /**
     * 进入多选界面
     */
    private void enterSelectInterface(){
        if (!isSelectMode){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeToolbarState(false);
            changeSelectbarState(false);
            changeFabState(false);
            changeFootbarState(false);
            changeCheckButtonState(false);
            setSelectAllButtonClickListener();
            isSelectMode = true;
        }
    }
    /**
     * 退出多选界面
     */
    private void quitSelectInterface(){
        if (isSelectMode){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeToolbarState(true);
            changeSelectbarState(true);
            changeFabState(true);
            changeFootbarState(true);
            changeCheckButtonState(true);
            mSelectbarTextView.setText("已选择 项");
            mSelectAllButton.setBackgroundResource(R.drawable.selectall_button_black);
            isSelectAll = false;
            isSelectMode = false;
        }
    }
    /**
     * @param position 传入Item对应的position
     * 进入长按菜单界面
     */
    private void enterSingleItemMenu(ItemViewHolder viewHolder, int position){
        if (!isSingleItemMenuMode){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeFabState(false);
            popSingleItemMenuWindow(viewHolder, position);
            isSingleItemMenuMode = true;
        }
    }
    /**
     * 退出长按菜单界面
     */
    private void quitSingleItemMenu(){
        if (isSingleItemMenuMode){
            TransitionManager.beginDelayedTransition(mRootView, mChangeBounds);
            changeFabState(true);
            dimissSingleItemMenuWindow();
            isSingleItemMenuMode = false;
        }
    }
    @Override
    protected void onResume(){
        //Log.d("mxrt", "onResume");
        super.onResume();
    }
    @Override
    protected void onRestart(){
        //Log.d("mxrt", "onRestart");
        super.onRestart();
        if (mOptionState != OPTION_IMPORT_PDF && mOptionState != OPTION_IMPORT_IMG){
            scanNewPicDir();
        }
        //非初次启动时扫描是否有新增文件
        if (isClickItemToPicViewActivity && !mScanedFileDirs.isEmpty() && mOptionState != OPTION_DELETE_ITEM){
            updatePreviewPic();
            updateOpenJustNowHint();
            isClickItemToPicViewActivity = false;
        }
        mOptionState = OPTION_DO_NOTHING;
    }
    private void scanNewPicDir(){
        if (mIsRecyclerLoaded && !mFisrtLoadThread.isAlive()){
            scanPictureFileNames();
            addFiltedFileName(mNeedToScanFileNames, true);
            mScanPicAccesser.reScan();

        }
    }
    /**
     * 更新刚点击的Item的预览图。
     */
    private void updatePreviewPic(){
        String scanedDirName = mScanedFileDirs.get(mClickedItemPosition);
        String firstPicFilePath = getFirstPicAbsPath(scanedDirName, null);
        PicInfo firstPicInfo = mPreviewPicInfos.get(mClickedItemPosition);
        String oldPath = firstPicInfo.path;
        if ((firstPicFilePath != null && !oldPath.equals(firstPicFilePath)) || isForceUpdatePreview){
            isForceUpdatePreview = false;
            Bitmap firstPic = IOUtils.getAbridgeBitmap(firstPicFilePath, PREVIEW_PIC_INSAMPLE_SIZE);
            firstPicInfo.bitmap = firstPic;
            firstPicInfo.path = firstPicFilePath;
            mRecyclerView.post(() -> {mAdapter.notifyItemChanged(mClickedItemPosition);});
        }
    }
    /**
     * 为刚刚点击打开的Item项的打上一个"刚刚打开"的提示标签
     */
    private void updateOpenJustNowHint(){
        if (mLastClickedItemPosition != mClickedItemPosition && mLastClickedItemPosition >= 0){
            mAdapter.notifyItemChanged(mLastClickedItemPosition, STATE_JUST_NOW_RESET);
        }
        mAdapter.notifyItemChanged(mClickedItemPosition, STATE_JUST_NOW);
    }
    /**
     * 扫描私有文件夹下的新增非空目录。同时删除空目录
     */
    private void scanPictureFileNames(){
        File privateFile = getExternalFilesDir("");
        mExternalFilePath = privateFile.getPath();
        String[] scanedFileNames = privateFile.list();
        for (String scanFileName : scanedFileNames){
            File scanFile = getExternalFilesDir(scanFileName);
            if (scanFile.list().length != 0){
                mNeedToScanFileNames.add(scanFileName);
            }else{
                //删空文件夹
                IOUtils.deleteDirectoryOrFile(getExternalFilesDir(scanFileName).getPath());
            }
        }
        Collections.sort(mNeedToScanFileNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return -(o1.compareTo(o2));
            }
        });
    }
    /**
     * @param dirs 要解析的目录列表，目录不一定是"scam_"开头的。
     * @param isFromHeadToAdd true表示从顶部开始添加，false表示从底部开始添加
     * 将满足"scam_"开头的目录保存起来。同时将目录对应的预览图信息解析出来，并通知更新Item项。
     */
    private void addFiltedFileName(List<String> dirs, boolean isFromHeadToAdd){
        //此处fileNames是应用私有files文件夹下的所有文件夹名
        for (String dir : dirs){
            if (dir.startsWith(SCAM_TAG) && !mScanedFileDirs.contains(dir)){
                if(parsePreviewPicture(dir, isFromHeadToAdd)){
                    if (isFromHeadToAdd){
                        mScanedFileDirs.add(0, dir);
                    }else{
                        mScanedFileDirs.add(dir);
                    }
                    if (mIsRecyclerLoaded){
                        mRecyclerView.post(()->{
                            if (isFromHeadToAdd){
                                //mAdapter.notifyItemInserted(0);
                                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
                                mRecyclerView.scrollToPosition(0);
                            }else{
                                mAdapter.notifyItemInserted(mAdapter.getItemCount());
                                //mAdapter.notifyItemChanged(mAdapter.getItemCount());
                            }
                        });
                    }
                }
            }
        }
    }
    /**
     * @param dir 给定的"scam_"开头的目录
     * @param isFromHeadAdd true表示从头开始添加，false表示从底部开始添加
     * 给定"scam_"开头的目录，解析出预览图信息
     */
    private boolean parsePreviewPicture(String dir, boolean isFromHeadAdd){
        String scanedFilePath = mExternalFilePath + mPathSeperator + dir;
        File scanedFile = new File(scanedFilePath);
        String[] picNames = scanedFile.list();//如果文件指向的目录不存在，这个鬼东西会返回null
        if (picNames != null && picNames.length != 0){
            String defaultFirstPicPath = scanedFilePath + "/" + picNames[0];
            String firstPicPath = getFirstPicAbsPath(dir, defaultFirstPicPath);
            Bitmap firstPicBitmap = IOUtils.getAbridgeBitmap(firstPicPath, PREVIEW_PIC_INSAMPLE_SIZE);
            PicInfo picInfo = new PicInfo();
            picInfo.name = picNames[0];
            picInfo.path = firstPicPath;
            picInfo.bitmap = firstPicBitmap;
            if (isFromHeadAdd){
                mPreviewPicInfos.add(0, picInfo);
            }else{
                mPreviewPicInfos.add(picInfo);
            }
        }else{
            return false;
        }
        return true;
    }
    /**
     * @param scanDir 的格式是"scam_xxxx"
     * @param defaultPath 如果在SharedPreferences中找不到满足条件的路径，返回该默认路径。默认路径应该也传入一个完整路径。
     * 给定格式为"scam_xxxx"的目录，返回该目录下第一张图的完整路径。这里面的顺序是以Item缓存的顺序为准。
     */
    private String getFirstPicAbsPath(String scanDir, String defaultPath){
        SharedPreferences preferences = getSharedPreferences(scanDir, Context.MODE_PRIVATE);
        Map<String, ?> picOrderderMap = preferences.getAll();
        for(Map.Entry<String, ?> entry : picOrderderMap.entrySet()){
            Object value = entry.getValue();
            if (value instanceof Integer){
                int order = (Integer)(value);
                if (order == 0){
                    return entry.getKey();
                }
            }
        }
        return defaultPath;
    }
    /**
     * @param position 指定对应的Item项
     * 按顺序返回Item项下所有图片的完整路径。
     */
    private List<String> getItemPicAbsPaths(int position){
        List<String> resultList = new ArrayList<>();
        SharedPreferences itemPreferences = getItemPreferences(position);
        Map<String, ?> picOrderMap = itemPreferences.getAll();
        String[] resultStringArray = new String[picOrderMap.size()];
        for (Map.Entry entry : picOrderMap.entrySet()){
            Object value = entry.getValue();
            if (value instanceof Integer){
                resultStringArray[(int)value] = (String)entry.getKey();
            }
        }
        for (String path : resultStringArray){
            if (path != null){
                resultList.add(path);
            }
        }
        if (resultList.size() == 0){
            return IOUtils.getSpecSufixFilePaths(getItemPicDirAbsPath(position), new String[]{"jpg"});
        }
        return resultList;
    }
    /**
     * @param position 指定对应的Item项
     * 返回Item项对应的SharedPreferences
     */
    private SharedPreferences getItemPreferences(int position){
        String itemDirName = getItemPicDirName(position);
        return getSharedPreferences(itemDirName, Context.MODE_PRIVATE);
    }
    /**
     * @param position 指定对应的Item项
     * @param format 时间戳转换成字符串的格式
     * 获取对应Item项的标题。
     */
    private String getItemTitle(int position, String format){
        String fileName = mScanedFileDirs.get(position);
        String timeStringFormated = getFileCreateTime(position, format);
        String defaultTitle = "新建文档 " + timeStringFormated;
        if (mPreferences.contains(fileName)){
            return mPreferences.getString(fileName, defaultTitle);
        }else{
            mPreferencesEditor.putString(fileName, defaultTitle);
            mPreferencesEditor.apply();
            return defaultTitle;
        }
    }

    public class PicInfo{
        public String path;
        public String name;
        public Bitmap bitmap;
    }
}