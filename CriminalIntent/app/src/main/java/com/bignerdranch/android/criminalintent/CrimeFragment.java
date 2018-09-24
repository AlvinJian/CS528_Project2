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

    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO= 2;

    private Crime mCrime;
    private File mPhotoFile, mPhotoFile1, mPhotoFile2, mPhotoFile3, mPhotoTemp;
    private EditText mTitleField;
    private Button mDateButton;
    private CheckBox mSolvedCheckbox;
    private Button mReportButton;
    private Button mSuspectButton;
    private ImageButton mPhotoButton;
    private CheckBox mCheckBox;
    private TextView mFaceNumText;
    private ImageView mPhotoView, mPhotoView1, mPhotoView2, mPhotoView3;
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
        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);
        mPhotoFile1= CrimeLab.get(getActivity()).getPhotoFile(mCrime, "IMG_" + "1" + ".jpg");
        mPhotoFile2= CrimeLab.get(getActivity()).getPhotoFile(mCrime, "IMG_" + "2" + ".jpg");
        mPhotoFile3= CrimeLab.get(getActivity()).getPhotoFile(mCrime, "IMG_" + "3" + ".jpg");
    }

    @Override
    public void onPause() {
        super.onPause();

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
                copyFromCurrent();
                startActivityForResult(captureImage, REQUEST_PHOTO);
            }
        });

        mPhotoView = (ImageView) v.findViewById(R.id.crime_photo);
        mCheckBox = (CheckBox) v.findViewById(R.id.face_detection_box);
        mFaceNumText = (TextView) v.findViewById(R.id.face_num_text);
        mPhotoView1 = (ImageView) v.findViewById(R.id.imageView1);
        mPhotoView2 = (ImageView) v.findViewById(R.id.imageView2);
        mPhotoView3 = (ImageView) v.findViewById(R.id.imageView3);
        updatePhotoView();
        firstPicture=mPhotoFile.exists()?false:true;
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
            movePhotoView();
            updatePhotoView();
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
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), getActivity());
            if (mCheckBox.isChecked())
            {
                PictureUtils.BitmapWithFaces bitmapWithFaces = PictureUtils.MarkFaces(bitmap, getContext());
                mPhotoView.setImageBitmap(bitmapWithFaces.bitmap);
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
                mPhotoView.setImageBitmap(bitmap);
                mFaceNumText.setText(" ");
            }
        }
        File[] f={mPhotoFile1, mPhotoFile2, mPhotoFile3};
        ImageView[] iView={mPhotoView1, mPhotoView2, mPhotoView3};
        for(int i=0;i<f.length;i++){
            if (f[i] == null || !f[i].exists()) {
                iView[i].setImageDrawable(null);
            } else {
                Bitmap bitmap = PictureUtils.getScaledBitmap(
                        f[i].getPath(), getActivity());
                iView[i].setImageBitmap(bitmap);
            }
        }



    }
    public void updatePhotoView(ImageView tempv, File f){
        if (f == null || !f.exists()) {
            tempv.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    f.getPath(), getActivity());
            tempv.setImageBitmap(bitmap);
        }
    }
    public void copyFromCurrent(){
        Date date=new Date();
        if(!firstPicture){
            mPhotoTemp=CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + "TempCurrent" + ".jpg");
//            mPhotoFile1=CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss a", new java.util.Date()) + ".jpg");
                try{
                    mPhotoTemp.createNewFile();
                    copyFile(mPhotoFile,mPhotoTemp);
                } catch (IOException e){
                    e.printStackTrace(); }

        }
    }
    private void movePhotoView() {


        if(mPhotoFile3!=null && mPhotoFile3.exists()) mPhotoFile3.delete();
        if(mPhotoFile2!=null && mPhotoFile2.exists()){
            mPhotoFile2.renameTo(CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + "3" + ".jpg"));
            mPhotoFile3=CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + "3" + ".jpg");
        }
        if(mPhotoFile1!=null && mPhotoFile1.exists()){
            mPhotoFile1.renameTo(CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + "2" + ".jpg"));
            mPhotoFile2=CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + "2" + ".jpg");
        }
        if(mPhotoTemp!=null && mPhotoTemp.exists()){
            mPhotoTemp.renameTo(CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + "1" + ".jpg"));
            mPhotoFile1=CrimeLab.get(getActivity()).getPhotoFile(mCrime,"IMG_" + "1" + ".jpg");
            mPhotoTemp.delete();
        }


    }

}
