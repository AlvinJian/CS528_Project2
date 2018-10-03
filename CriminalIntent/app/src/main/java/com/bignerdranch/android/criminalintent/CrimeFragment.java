package com.bignerdranch.android.criminalintent;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.UUID;
import java.util.Date;

public class CrimeFragment extends Fragment {
    private static final String TAG = "CrimeFragment";

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String IMAGE_TYPE = ".jpg";

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO= 2;

    private Crime mCrime;
    private File mPhotoFile, mPhotoFile1, mPhotoFile2, mPhotoFile3, mPhotoFile4, mPhotoTemp;
    private File[] mPhotoAry;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckbox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;
    private CheckBox mCheckBox;
    private TextView mFaceNumText;
    private ImageView mPhotoView, mPhotoView1, mPhotoView2, mPhotoView3, mPhotoView4;
    private boolean firstPicture=true;


    public static CrimeFragment newInstance(UUID crimeId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);
        mCrime.setImg_1(crimeId + "IMG_" + "1" + IMAGE_TYPE);
        mCrime.setImg_2(crimeId + "IMG_" + "2" + IMAGE_TYPE);
        mCrime.setImg_3(crimeId + "IMG_" + "3" + IMAGE_TYPE);
        mCrime.setImg_4(crimeId + "IMG_" + "4" + IMAGE_TYPE);
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
        mPhotoFile1= CrimeLab.get(getActivity()).getPhotoFile(mCrime, "IMG_" + "1" + IMAGE_TYPE);
        mPhotoFile2= CrimeLab.get(getActivity()).getPhotoFile(mCrime, "IMG_" + "2" + IMAGE_TYPE);
        mPhotoFile3= CrimeLab.get(getActivity()).getPhotoFile(mCrime, "IMG_" + "3" + IMAGE_TYPE);
        mPhotoFile4= CrimeLab.get(getActivity()).getPhotoFile(mCrime, "IMG_" + "4" + IMAGE_TYPE);
        mPhotoAry=new File[]{mPhotoFile1, mPhotoFile2, mPhotoFile3, mPhotoFile4};
    }

    @Override
    public void onPause() {
        super.onPause();
        //Log.i(TAG, "onPauseUPDATE*****************");
        CrimeLab.get(getActivity())
                .updateCrime(mCrime);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_crime, container, false);

        mTitleField = (EditText) v.findViewById(R.id.crime_title);
        mTitleField.setText(mCrime.getTitle());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mDateButton = (Button) v.findViewById(R.id.crime_date);
        updateDate();
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                DatePickerFragment dialog = DatePickerFragment
                        .newInstance(mCrime.getDate());
                dialog.setTargetFragment(CrimeFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mSolvedCheckbox = (CheckBox) v.findViewById(R.id.crime_solved);
        mSolvedCheckbox.setChecked(mCrime.isSolved());
        mSolvedCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mCrime.setSolved(isChecked);
            }
        });

        mReportButton = (Button)v.findViewById(R.id.crime_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject));
                i = Intent.createChooser(i, getString(R.string.send_report));

                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        mSuspectButton = (Button)v.findViewById(R.id.crime_suspect);
        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(pickContact, REQUEST_CONTACT);
            }
        });

        if (mCrime.getSuspect() != null) {
            mSuspectButton.setText(mCrime.getSuspect());
        }

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        }

        mPhotoButton = (ImageButton) v.findViewById(R.id.crime_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        if (canTakePhoto) {
            Uri uri = FileProvider.getUriForFile(getContext(),
                    "com.bignerdranch.android.criminalintent.ImageProvider",
                    mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        mPhotoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mCheckBox = (CheckBox) v.findViewById(R.id.face_detection_box);
        mCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatePhotoView();
            }
        });
        mFaceNumText = (TextView) v.findViewById(R.id.face_num_text);
        mPhotoView1 = (ImageView) v.findViewById(R.id.crime_photo1);
        mPhotoView2 = (ImageView) v.findViewById(R.id.crime_photo2);
        mPhotoView3 = (ImageView) v.findViewById(R.id.crime_photo3);
        mPhotoView4 = (ImageView) v.findViewById(R.id.crime_photo4);
        updatePhotoView();
        firstPicture=mPhotoFile1.exists()?false:true;
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_DATE) {
            Date date = (Date) data
                    .getSerializableExtra(DatePickerFragment.EXTRA_DATE);
            mCrime.setDate(date);
            updateDate();
        } else if (requestCode == REQUEST_CONTACT && data != null) {
            Uri contactUri = data.getData();
            // Specify which fields you want your query to return
            // values for.
            String[] queryFields = new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME,
            };
            // Perform your query - the contactUri is like a "where"
            // clause here
            ContentResolver resolver = getActivity().getContentResolver();
            Cursor c = resolver
                    .query(contactUri, queryFields, null, null, null);

            try {
                // Double-check that you actually got results
                if (c.getCount() == 0) {
                    return;
                }

                // Pull out the first column of the first row of data -
                // that is your suspect's name.
                c.moveToFirst();

                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                mSuspectButton.setText(suspect);
            } finally {
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO) {
            mCrime.setFaceDetectIndex(mCrime.getCurrentImagePosition());
            updatePhotoView();
            mCrime.setCurrentImagePosition((mCrime.getCurrentImagePosition()+1)%4);
            Log.d("CurrentImagePosition", ""+mCrime.getCurrentImagePosition());
            firstPicture=false;

        }
    }
    private void copyFile(File sourceFile, File destFile) throws IOException{
        if(!sourceFile.exists()) return;
        FileChannel source=null;
        FileChannel destination=null;
        source=new FileInputStream(sourceFile).getChannel();
        destination=new FileOutputStream(destFile).getChannel();
        if(destination!=null && source!=null){
            destination.transferFrom(source,0,source.size());
        }
        if(source!=null) source.close();
        if(destination!=null) destination.close();
    }

    private void updateDate() {
        mDateButton.setText(mCrime.getDate().toString());
    }

    private String getCrimeReport() {
        String solvedString = null;
        if (mCrime.isSolved()) {
            solvedString = getString(R.string.crime_report_solved);
        } else {
            solvedString = getString(R.string.crime_report_unsolved);
        }
        String dateFormat = "EEE, MMM dd";
        String dateString = DateFormat.format(dateFormat, mCrime.getDate()).toString();
        String suspect = mCrime.getSuspect();
        if (suspect == null) {
            suspect = getString(R.string.crime_report_no_suspect);
        } else {
            suspect = getString(R.string.crime_report_suspect, suspect);
        }
        String report = getString(R.string.crime_report,
                mCrime.getTitle(), dateString, solvedString, suspect);
        return report;
    }
    
    private void updatePhotoView() {
        int currentImagePosition = mCrime.getCurrentImagePosition();
        int faceDetectIndex = mCrime.getFaceDetectIndex();

        if(mPhotoFile!=null && mPhotoFile.exists()){
            mPhotoAry[currentImagePosition].delete();
            try{
                mPhotoAry[currentImagePosition].createNewFile();
                copyFile(mPhotoFile,mPhotoAry[currentImagePosition]);
                mPhotoFile.delete();
            }catch (IOException e){
                e.printStackTrace(); }

        }

        ImageView[] iView={mPhotoView1, mPhotoView2, mPhotoView3, mPhotoView4};
        for(int i=0;i<mPhotoAry.length;i++){
            if (mPhotoAry[i] == null || !mPhotoAry[i].exists()) {
                iView[i].setImageDrawable(null);
            } else {
                Bitmap bitmap = PictureUtils.getScaledBitmap(
                        mPhotoAry[i].getPath(), getActivity());
                iView[i].setImageBitmap(bitmap);
            }
        }
        if(mPhotoAry[faceDetectIndex] !=null && mPhotoAry[faceDetectIndex].exists()){
            Bitmap bitmap = PictureUtils.getScaledBitmap
                    (mPhotoAry[faceDetectIndex].getPath(), getActivity());
            if (mCheckBox.isChecked())
            {
                PictureUtils.BitmapWithFaces bitmapWithFaces = PictureUtils.MarkFaces(bitmap, getContext());
                bitmap.recycle();
                iView[faceDetectIndex].setImageBitmap(bitmapWithFaces.bitmap);
                StringBuilder builder = new StringBuilder();
                builder.append(bitmapWithFaces.faces.size());
                builder.append(" ");
                builder.append(getContext().getResources().getString(R.string.faces_text_stub));
                String text = builder.toString();
                mFaceNumText.setText(text);
                Log.d(TAG, text);
            }
            else
            {
                iView[faceDetectIndex].setImageBitmap(bitmap);
                mFaceNumText.setText(" ");
            }
        }

    }


}
