package com.nerd.criminalintent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String AGR_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT_NAME = 1;
    private static final int REQUEST_CONTACT_PHONE = 2;
    private static final int REQUEST_PHOTO = 3;

    private Crime mCrime;
    private File mPhotoFile;
    private ImageView mPhotoView;
    private ImageButton mPhotoButton;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckBox;
    private Button mReportButton;
    private Button mSuspectButton;
    private Button mCallButton;
    private Callbacks mCallbacks;

    public interface Callbacks {
        void onCrimeUpdated(Crime crime);

        void onCrimeRemove(Crime crime);
    }

    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(AGR_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        UUID id = (UUID) getArguments().getSerializable(AGR_CRIME_ID);
        mCrime = CrimeLab.get(getContext()).getCrime(id);
        mPhotoFile = CrimeLab.get(getContext()).getPhotoFile(mCrime);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        PackageManager packageManager = getActivity().getPackageManager();

        View view = inflater.inflate(R.layout.fragment_crime, container, false);

        mPhotoView = view.findViewById(R.id.crime_photo);
        ViewTreeObserver observer = mPhotoView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updatePhotoView();
            }
        });
        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mPhotoFile && mPhotoFile.exists()) {
                    ImageZoomDialog picturePreviewDialog = ImageZoomDialog.newInstance(
                            mPhotoFile.getPath());
                    picturePreviewDialog.show(getFragmentManager(), CrimeFragment.class.getSimpleName());
                }
            }
        });

        mPhotoButton = view.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null && captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if (canTakePhoto) {
            Uri uri = Uri.fromFile(mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            mPhotoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivityForResult(captureImage, REQUEST_PHOTO);
                }
            });
        }

        mTitleField = view.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
                updateCrime();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = view.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getResources().getBoolean(R.bool.is_table)) {
                    DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(mCrime.getDate());
                    datePickerDialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                    datePickerDialog.show(getFragmentManager(), DIALOG_DATE);
                } else {
                    Intent intent = DatePickerActivity.newIntent(getContext(), mCrime.getDate());
                    startActivityForResult(intent, REQUEST_DATE);
                }
            }
        });

        mSolvedCheckBox = view.findViewById(R.id.crime_solved);
        mSolvedCheckBox.setChecked(mCrime.isSolved());
        mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
                updateCrime();
            }
        });

        mReportButton = view.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity());
                builder.setType("text/plain")
                        .setSubject(getString(R.string.crime_report_subject))
                        .setText(getCrimeReport())
                        .startChooser();
            }
        });

        mSuspectButton = view.findViewById(R.id.crime_suspect);
        final Intent pickContactData = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        boolean canDisplayName = pickContactData.resolveActivity(packageManager) != null;
        mSuspectButton.setEnabled(canDisplayName);

        if (canDisplayName) {
            mSuspectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(pickContactData, REQUEST_CONTACT_NAME);
                }
            });
        }

        if (null != mCrime.getSuspect()) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        mCallButton = view.findViewById(R.id.crime_call);
        final Intent pickContactPhone = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        boolean canDisplayNumber = pickContactPhone.resolveActivity(packageManager) != null;
        mCallButton.setEnabled(canDisplayNumber);

        if (canDisplayNumber) {
            mCallButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(pickContactPhone, REQUEST_CONTACT_PHONE);
                }
            });
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.menu_item_remove_crime:
                removeCrime();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.get(getContext()).updateCrime(mCrime);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode) {
            return;
        }
        switch (requestCode) {
            case REQUEST_DATE:
                Date date = (Date) data
                        .getSerializableExtra(DatePickerDialog.EXTRA_DATE);
                mCrime.setDate(date);
                updateCrime();
                updateDate();
                break;
            case REQUEST_CONTACT_NAME:
                if (null != data) {
                    Uri contactUri = data.getData();
                    String[] queryFields = new String[]{
                            ContactsContract.Contacts.DISPLAY_NAME};
                    Cursor cursor = getActivity().getContentResolver()
                            .query(contactUri, queryFields, null, null, null);
                    if (null == cursor || 0 == cursor.getCount()) {
                        return;
                    }

                    try {
                        cursor.moveToFirst();
                        String suspect = cursor.getString(0);
                        mCrime.setSuspect(suspect);
                        updateCrime();
                        mSuspectButton.setText(suspect);
                    } finally {
                        cursor.close();
                    }
                }
                break;
            case REQUEST_CONTACT_PHONE:
                if (null != data) {
                    Uri phoneUri = data.getData();
                    String[] queryFields = new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER};
                    Cursor cursor = getActivity().getContentResolver()
                            .query(phoneUri, queryFields, null, null, null);
                    if (null == cursor || 0 == cursor.getCount()) {
                        return;
                    }

                    try {
                        cursor.moveToFirst();
                        String phone = cursor.getString(0);
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } finally {
                        cursor.close();
                    }
                }
                break;
            case REQUEST_PHOTO:
                updatePhotoView();
                break;
        }
    }

    private void updateCrime() {
        CrimeLab.get(getActivity()).updateCrime(mCrime);
        if (null != mCallbacks) {
            mCallbacks.onCrimeUpdated(mCrime);
        }
    }

    private void removeCrime() {
        CrimeLab.get(getActivity()).remove(mCrime);
        if (null != mCallbacks) {
            mCallbacks.onCrimeRemove(mCrime);
        }
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getFormatDate());
    }

    private String getCrimeReport() {
        String dateString = mCrime.getFormatDate();

        String solvedString;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_unsolved);
        } else {
            solvedString = getString(R.string.crime_report_solved);
        }

        String suspect = mCrime.getSuspect();
        if (TextUtils.isEmpty(suspect)) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }

        return getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
    }

    private void updatePhotoView() {
        if (null == mPhotoFile || !mPhotoFile.exists()) {
            mPhotoView.setImageBitmap(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), mPhotoView.getWidth(), mPhotoView.getHeight());
            mPhotoView.setImageBitmap(bitmap);
        }
    }
}
