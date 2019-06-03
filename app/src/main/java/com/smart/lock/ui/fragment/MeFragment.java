
package com.smart.lock.ui.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.BuildConfig;
import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.UserProfile;
import com.smart.lock.db.dao.UserProfileDao;
import com.smart.lock.ui.AboutUsActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.UserManagerActivity;
import com.smart.lock.ui.setting.DeviceManagementActivity;
import com.smart.lock.ui.setting.SystemSettingsActivity;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.FileUtil;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.CustomDialog;
import com.smart.lock.widget.MeDefineView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;

public class MeFragment extends BaseFragment implements View.OnClickListener {
    private View mMeView;
    private MeDefineView mSystemSetTv;
    private MeDefineView mDevManagementTv;
    private MeDefineView mAboutUsTv;
    private TextView mNameTv;
    private ImageView mEditNameIv;
    private ImageView mHeadPhoto;

    private UserProfile mUserProfile;

    private TextView mCameraShotTv;
    private TextView mLocalPhotoTv;
    private TextView mGalleryPhotoTv;
    private TextView mCancelTv;

    private BottomSheetDialog mBottomSheetDialog; //头像选择
    private Dialog mEditorNameDialog;
    private static final int REQUEST_TAKE_PHOTO = 100; // 拍照并进行裁剪
    private static final int REQUEST_CROP = 101; // 裁剪后设置图片
    private static final int SCAN_OPEN_PHONE = 102; // 打开图库获取图片并进行裁剪
    private static final String IMAGE_FILE_NAME = "head.jpg";

    private Uri imgUri; // 拍照时返回的uri
    private Uri mCutUri;// 图片裁剪时返回的uri
    private File imgFile;// 拍照保存的图片文件

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View initView() {
        mMeView = View.inflate(mActivity, R.layout.me_fragment, null);
        mSystemSetTv = mMeView.findViewById(R.id.system_set);
        mDevManagementTv = mMeView.findViewById(R.id.mc_manage);
        mAboutUsTv = mMeView.findViewById(R.id.about_us);
        mNameTv = mMeView.findViewById(R.id.me_center_head_name);
        mEditNameIv = mMeView.findViewById(R.id.me_edit_name);
        mHeadPhoto = mMeView.findViewById(R.id.me_center_head_photo);

        mBottomSheetDialog = DialogUtils.createBottomSheetDialog(mMeView.getContext(), R.layout.bottom_sheet_set_head_photo, R.id.design_bottom_sheet);
        mCameraShotTv = mBottomSheetDialog.findViewById(R.id.camera_shot_tv);
        mLocalPhotoTv = mBottomSheetDialog.findViewById(R.id.local_photo_tv);
        mGalleryPhotoTv = mBottomSheetDialog.findViewById(R.id.gallery_photo_tv);
        mCancelTv = mBottomSheetDialog.findViewById(R.id.tv_cancel);

        mUserProfile = UserProfileDao.getInstance(mMeView.getContext()).queryById(1);
        LogUtil.d(TAG, "mUserProfile : " + ((mUserProfile == null) ? true : mUserProfile.toString()));
        if (mUserProfile == null) {
            mUserProfile = new UserProfile();
            mNameTv.setText(mMeView.getContext().getString(R.string.no_name));
            mUserProfile.setUserName(mMeView.getContext().getString(R.string.no_name));
            UserProfileDao.getInstance(mMeView.getContext()).insert(mUserProfile);
        } else {
            mNameTv.setText(mUserProfile.getUserName());
            if (StringUtil.checkNotNull(mUserProfile.getPhotoPath())) {
                Bitmap bitmap = BitmapFactory.decodeFile(mUserProfile.getPhotoPath());
                if (bitmap != null) {
                    mHeadPhoto.setImageBitmap(bitmap);
                }
            }
        }
        mEditorNameDialog = DialogUtils.createEditorDialog(mActivity, getString(R.string.modify_name), mNameTv.getText().toString());
        initEvent();
        return mMeView;
    }

    public void initDate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {  //版本检测
            Toolbar mToolbar = mMeView.findViewById(R.id.tb_toolbar);
            ((AppCompatActivity) Objects.requireNonNull(getActivity())).setSupportActionBar(mToolbar);  //将ToolBar设置成ActionBar
        }
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        mSystemSetTv.setDes(mMeView.getContext().getResources().getString(R.string.system_setting));
        mSystemSetTv.setImage(R.mipmap.ic_setting);
        mDevManagementTv.setDes(mMeView.getResources().getString(R.string.device_management));
        mDevManagementTv.setImage(R.mipmap.ic_device_management);
        mAboutUsTv.setDes(mMeView.getResources().getString(R.string.about_us));

        mAboutUsTv.setImage(R.mipmap.ic_about_us);
        mAboutUsTv.setVisibility(View.GONE);
        if (mDefaultUser != null) {
            mNameTv.setText(mDefaultUser.getUserName());
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_me, menu);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchDev();
                break;
            case R.id.action_scan:
                if (mActivity instanceof OnFragmentInteractionListener) {
                    ((OnFragmentInteractionListener) mActivity).onScanQrCode();
                }
                break;
            default:
                break;

        }
        return true;
    }

    private void initEvent() {
        mSystemSetTv.setOnClickListener(this);
        mDevManagementTv.setOnClickListener(this);
        mAboutUsTv.setOnClickListener(this);
        mEditNameIv.setOnClickListener(this);
        mHeadPhoto.setOnClickListener(this);
        mCameraShotTv.setOnClickListener(this);
        mLocalPhotoTv.setOnClickListener(this);
        mGalleryPhotoTv.setOnClickListener(this);
        mNameTv.setOnClickListener(this);
        mCancelTv.setOnClickListener(this);
        //修改呢称响应事件
        mEditorNameDialog.findViewById(R.id.dialog_confirm_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newName = ((EditText) mEditorNameDialog.findViewById(R.id.editor_et)).getText().toString();
                if (!newName.isEmpty()) {
                    mNameTv.setText(newName);
                    mUserProfile.setUserName(newName);
                    UserProfileDao.getInstance(mActivity).update(mUserProfile);
                } else {
                    ToastUtil.showLong(mActivity, R.string.cannot_be_empty_str);
                }
                mEditorNameDialog.dismiss();
            }
        });
    }

    /**
     * 调用Activity中的函数
     */
    public interface OnFragmentInteractionListener {
        void onScanQrCode();
    }

    @SuppressLint("IntentReset")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mc_manage:
                Bundle bundle = new Bundle();
                if (mDefaultDevice != null) {
                    bundle.putSerializable(BleMsg.KEY_DEFAULT_DEVICE, mDefaultDevice);
                }
                startIntent(DeviceManagementActivity.class, bundle);
                break;
            case R.id.system_set:
                Intent intent = new Intent(mMeView.getContext(), SystemSettingsActivity.class);
                this.startActivity(intent);
                break;
            case R.id.about_us:
                Intent aboutIntent = new Intent(mMeView.getContext(), AboutUsActivity.class);
                this.startActivity(aboutIntent);
                break;
            case R.id.me_center_head_name:
            case R.id.me_edit_name:
                ((EditText) mEditorNameDialog.findViewById(R.id.editor_et)).setText(mNameTv.getText());
                mEditorNameDialog.show();
                break;
            case R.id.me_center_head_photo:
                mBottomSheetDialog.show();
                break;
            case R.id.camera_shot_tv:
                if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    imageCapture();
                } else {
                    requestPermissions(mExternalPermission, REQUESTCODE);
                }
                mBottomSheetDialog.cancel();
                break;
            case R.id.local_photo_tv:
                mBottomSheetDialog.cancel();
                break;
            case R.id.gallery_photo_tv:
                if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Intent picture = new Intent(Intent.ACTION_PICK);
                    picture.setType("image/*");
                    // 判断系统中是否有处理该 Intent 的 Activity
                    if (picture.resolveActivity(mMeView.getContext().getPackageManager()) != null) {
                        startActivityForResult(picture, SCAN_OPEN_PHONE);
                    } else {
                        Toast.makeText(mMeView.getContext(), "未找到图片查看器", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    requestPermissions(mExternalPermission, REQUESTCODE);
                }
                mBottomSheetDialog.cancel();
                break;
            case R.id.tv_cancel:
                mBottomSheetDialog.cancel();
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogUtil.d(TAG, "requestCode : " + requestCode);
        switch (requestCode) {
            case REQUESTCODE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                            || !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        imageCapture();
                    }
                }
                break;
            case REQUESTCODE_CROP_PHOTO:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            || !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                            || !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Intent picture = new Intent(Intent.ACTION_PICK);
                        picture.setType("image/*");
                        // 判断系统中是否有处理该 Intent 的 Activity
                        if (picture.resolveActivity(mMeView.getContext().getPackageManager()) != null) {
                            startActivityForResult(picture, SCAN_OPEN_PHONE);
                        } else {
                            Toast.makeText(mMeView.getContext(), "未找到图片查看器", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }

    }

    // 从file中获取uri
    private static Uri getUriForFile(Context context, File file) {
        if (context == null || file == null) {
            throw new NullPointerException();
        }
        Uri pictureUri;
        if (Build.VERSION.SDK_INT >= 24) {
            pictureUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".FileProvider", file);
        } else {
            pictureUri = Uri.fromFile(file);
        }
        return pictureUri;
    }

    private void searchDev() {
        startIntent(LockDetectingActivity.class, null);
    }

    /**
     * 判断系统及拍照
     */
    private void imageCapture() {
        String dir = FileUtil.createDir(mMeView.getContext(), "smallIcon") + File.separator;
        imgFile = new File(dir, IMAGE_FILE_NAME);
        imgUri = getUriForFile(mMeView.getContext(), imgFile);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 回调成功
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                // 拍照并进行裁剪
                case REQUEST_TAKE_PHOTO:
                    cropPhoto(imgUri, true);
                    break;

                // 裁剪后设置图片
                case REQUEST_CROP:
                    Bitmap bitmap = BitmapFactory.decodeFile(mCutUri.getPath());
                    if (bitmap != null) {
                        mHeadPhoto.setImageBitmap(bitmap);
                    }
                    mUserProfile.setPhotoPath(mCutUri.getPath());
                    UserProfileDao.getInstance(mActivity).update(mUserProfile);
                    break;
                // 打开图库获取图片并进行裁剪
                case SCAN_OPEN_PHONE:
                    cropPhoto(data.getData(), false);
                    break;
            }
        }
    }


    /**
     * 小图模式中，保存图片后，设置到视图中
     */
    private void setPicToView(Intent data) {
        LogUtil.d(TAG, "setPicToView : " + (data == null));
        Bundle extras = data.getExtras();
        if (extras != null) {
            Bitmap photo = extras.getParcelable("data"); // 直接获得内存中保存的 bitmap
            // 创建 smallIcon 文件夹
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String storage = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "SmartLock_DT" + File.separator + "smallIcon";
                File dirFile = new File(storage);
                if (!dirFile.exists()) {
                    if (!dirFile.mkdirs()) {
                    } else {
                    }
                }
                File file = new File(dirFile, System.currentTimeMillis() + ".jpg");
                // 保存图片

                Uri uri = Uri.fromFile(file);
                mUserProfile.setPhotoPath(uri.getPath());
                UserProfileDao.getInstance(mActivity).update(mUserProfile);
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 在视图中显示图片
            mHeadPhoto.setImageBitmap(photo);
        }
    }

    // 图片裁剪
    private void cropPhoto(Uri uri, boolean fromCapture) {
        Intent intent = new Intent("com.android.camera.action.CROP"); //打开系统自带的裁剪图片的intent


        // 注意一定要添加该项权限，否则会提示无法裁剪
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent.setDataAndType(uri, "image/*");
        intent.putExtra("scale", true);

        // 设置裁剪区域的宽高比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);

        // 设置裁剪区域的宽度和高度
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);

        // 取消人脸识别
        intent.putExtra("noFaceDetection", true);
        // 图片输出格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        // 若为false则表示不返回数据
        intent.putExtra("return-data", false);

        // 指定裁剪完成以后的图片所保存的位置,pic info显示有延时
        if (fromCapture) {
            // 如果是使用拍照，那么原先的uri和最终目标的uri一致,注意这里的uri必须是Uri.fromFile生成的
            mCutUri = Uri.fromFile(imgFile);
        } else { // 从相册中选择，那么裁剪的图片保存在take_photo中
            String time = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
            String fileName = "photo_" + time;
            File mCutFile = new File(Environment.getExternalStorageDirectory() + File.separator + "SmartLock_DT" + File.separator + "smallIcon", fileName + ".jpeg");
            if (!mCutFile.getParentFile().exists()) {
                mCutFile.getParentFile().mkdirs();
            }
            mCutUri = Uri.fromFile(mCutFile);

        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mCutUri);
        // 以广播方式刷新系统相册，以便能够在相册中找到刚刚所拍摄和裁剪的照片
        Intent intentBc = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intentBc.setData(uri);
        mMeView.getContext().sendBroadcast(intentBc);

        startActivityForResult(intent, REQUEST_CROP); //设置裁剪参数显示图片至ImageVie
    }
}
