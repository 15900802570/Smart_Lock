
package com.smart.lock.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.smart.lock.R;
import com.smart.lock.ble.BleMsg;
import com.smart.lock.db.bean.UserProfile;
import com.smart.lock.db.dao.UserProfileDao;
import com.smart.lock.ui.AboutUsActivity;
import com.smart.lock.ui.LockDetectingActivity;
import com.smart.lock.ui.setting.DeviceManagementActivity;
import com.smart.lock.ui.setting.SystemSettingsActivity;
import com.smart.lock.utils.DialogUtils;
import com.smart.lock.utils.LogUtil;
import com.smart.lock.utils.StringUtil;
import com.smart.lock.widget.MeDefineView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Objects;

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
    }

    /**
     * 调用Activity中的函数
     */
    public interface OnFragmentInteractionListener {
        void onScanQrCode();
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
            case R.id.me_center_head_name:
            case R.id.me_edit_name:
//                showEditDialog(mMeView.getContext(), getString(R.string.modify_note_name)).show();
                DialogUtils.createEditorDialog(mActivity, "修改呢称",mNameTv.getText().toString());
                break;
            case R.id.me_center_head_photo:
//                mBottomSheetDialog.show();
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
            case R.id.tv_cancel:
                mBottomSheetDialog.cancel();
                break;
            default:
                break;
        }
    }

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
                if (StringUtil.checkIsNull(name)) {
                    name = context.getString(R.string.no_name);
                }
                mNameTv.setText(name);
                mUserProfile.setUserName(name);
                UserProfileDao.getInstance(context).update(mUserProfile);
            }
        });
        return builder.create();
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

}
