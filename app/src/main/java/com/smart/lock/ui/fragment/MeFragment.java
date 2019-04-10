
package com.smart.lock.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.DeviceUser;
import com.smart.lock.db.bean.UserProfile;
import com.smart.lock.db.dao.DeviceInfoDao;
import com.smart.lock.db.dao.DeviceUserDao;
import com.smart.lock.db.dao.UserProfileDao;
import com.smart.lock.ui.AboutUsActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.setting.DeviceManagementActivity;
import com.smart.lock.ui.setting.SystemSettingsActivity;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.ToastUtil;
import com.smart.lock.widget.MeDefineView;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;
import com.yzq.zxinglibrary.common.Constant;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
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

    private String mSn; //设备SN
    private String mNodeId; //设备IMEI
    private String mBleMac; //蓝牙地址
    private UserProfile mUserProfile;

    private TextView mCameraShotTv;
    private TextView mLocalPhotoTv;
    private TextView mGalleryPhotoTv;

    private BottomSheetDialog mBottomSheetDialog; //头像选择

    private static final int REQUEST_CODE_SCAN = 0;

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

        mUserProfile = UserProfileDao.getInstance(mMeView.getContext()).queryById(1);
        LogUtil.d(TAG, "mUserProfile : " + ((mUserProfile == null) ? true : mUserProfile.toString()));
        if (mUserProfile == null) {
            mUserProfile = new UserProfile();
            mNameTv.setText(mMeView.getContext().getString(R.string.no_name));
            mUserProfile.setUserName(mMeView.getContext().getString(R.string.no_name));
            UserProfileDao.getInstance(mMeView.getContext()).insert(mUserProfile);
        } else {
            mNameTv.setText(mUserProfile.getUserName());
        }
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

    public void saveBitmapFile(Bitmap bitmap, String path) {
        File file = new File(path);//将要保存图片的路径
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchDev();
                break;
            case R.id.action_scan:
                scanQr();
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                if(getActivity() instanceof OnFragmentInteractionListener){
                    ((OnFragmentInteractionListener) getActivity()).onScanForResult(data);
                }
            }
        }
    }

    /**
     * 调用MainActivity中的函数
     */
    public interface  OnFragmentInteractionListener{
        void onScanForResult(Intent data);
    }

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
            case R.id.me_edit_name:
                showEditDialog(mMeView.getContext(), getString(R.string.modify_note_name)).show();
                break;
            case R.id.me_center_head_photo:
                mBottomSheetDialog.show();
                break;
            case R.id.camera_shot_tv:
                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera, 1);
                mBottomSheetDialog.cancel();
                break;
            case R.id.local_photo_tv:
                mBottomSheetDialog.cancel();
                break;
            case R.id.gallery_photo_tv:
                Intent picture = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(picture, 2);
                mBottomSheetDialog.cancel();
                break;
            default:
                break;
        }
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 1 && resultCode == Activity.RESULT_OK
//                && null != data) {
//            String sdState = Environment.getExternalStorageState();
//            if (!sdState.equals(Environment.MEDIA_MOUNTED)) {
//                return;
//            }
//            new DateFormat();
//            String name = DateFormat.format("yyyyMMdd_hhmmss",
//                    Calendar.getInstance(Locale.CHINA)) + ".jpg";
//            Bundle bundle = data.getExtras();
//            // 获取相机返回的数据，并转换为图片格式
//            Bitmap bmp = (Bitmap) bundle.get("data");
//            FileOutputStream fout = null;
//            String filename = null;
//            try {
//                filename = UtilImags.SHOWFILEURL(mMeView.getContext()) + "/" + name;
//            } catch (IOException e) {
//            }
//            try {
//                fout = new FileOutputStream(filename);
//                bmp.compress(Bitmap.CompressFormat.JPEG, 100, fout);
//            } catch (FileNotFoundException e) {
//                ToastUtil.show(mMeView.getContext(), "上传失败", Toast.LENGTH_LONG);
//            } finally {
//                try {
//                    fout.flush();
//                    fout.close();
//                } catch (IOException e) {
//                    ToastUtil.show(mMeView.getContext(), "上传失败", Toast.LENGTH_LONG);
//                }
//            }
//            zqRoundOvalImageView.setImageBitmap(bmp);
//            staffFileupload(new File(filename));
//        }
//        if (requestCode == 2 && resultCode == Activity.RESULT_OK
//                && null != data) {
//            try {
//                Uri selectedImage = data.getData();
//                String[] filePathColumns = {MediaStore.Images.Media.DATA};
//                Cursor c = mMeView.getContext().getContentResolver().query(selectedImage,
//                        filePathColumns, null, null, null);
//                c.moveToFirst();
//                int columnIndex = c.getColumnIndex(filePathColumns[0]);
//                String picturePath = c.getString(columnIndex);
//                c.close();
//
//                Bitmap bmp = BitmapFactory.decodeFile(picturePath);
//                // 获取图片并显示
//                zqRoundOvalImageView.setImageBitmap(bmp);
//                saveBitmapFile(UtilImags.compressScale(bmp), UtilImags.SHOWFILEURL(mMeView.getContext()) + "/stscname.jpg");
//                staffFileupload(new File(UtilImags.SHOWFILEURL(mMeView.getContext()) + "/stscname.jpg"));
//            } catch (Exception e) {
//                ToastUtil.show(mMeView.getContext(), "上传失败", Toast.LENGTH_LONG);
//            }
//        }
//    }

    private AlertDialog showEditDialog(final Context context, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final EditText editText = new EditText(context);
        builder.setTitle(msg)
                .setView(editText)
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = editText.getText().toString().trim();
                mNameTv.setText(name);
                mUserProfile.setUserName(name);
                UserProfileDao.getInstance(context).update(mUserProfile);
            }
        });
        AlertDialog dialog = builder.create();
        return dialog;
    }

    public void setOnClick(View view) {
        switch (view.getId()) {
            case R.id.camera_shot_tv:
                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera, 1);
                break;
            case R.id.local_photo_tv:
                break;
            case R.id.gallery_photo_tv:
                Intent picture = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(picture, 2);
                break;
            default:
                break;
        }
        mBottomSheetDialog.cancel();
    }

    private void searchDev() {
//        Bundle bundle = new Bundle();

        startIntent(LockDetectingActivity.class, null);
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    /**
     * 打开第三方二维码扫描库
     */
    private void scanQr() {
        Intent newIntent = new Intent(mMeView.getContext(), CaptureActivity.class);
        ZxingConfig config = new ZxingConfig();
        config.setPlayBeep(true);//是否播放扫描声音 默认为true
        config.setShake(true);//是否震动  默认为true
        config.setDecodeBarCode(false);//是否扫描条形码 默认为true
        config.setReactColor(R.color.colorAccent);//设置扫描框四个角的颜色 默认为淡蓝色
        config.setFrameLineColor(R.color.colorAccent);//设置扫描框边框颜色 默认无色
        config.setFullScreenScan(true);//是否全屏扫描  默认为true  设为false则只会在扫描框中扫描
        newIntent.putExtra(Constant.INTENT_ZXING_CONFIG, config);
        startActivityForResult(newIntent, REQUEST_CODE_SCAN);
    }
}
