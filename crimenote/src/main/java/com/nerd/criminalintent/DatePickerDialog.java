package com.nerd.criminalintent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DatePickerDialog extends DialogFragment {

    public static final String EXTRA_DATE = DatePickerDialog.class.getPackage().getName() + ".DATE";

    private static final String ARG_DATE = "date";

    private DatePicker mDatePicker;
    private TextView mConfirmTextView;

    public static DatePickerDialog newInstance(Date date) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_DATE, date);

        DatePickerDialog dialog = new DatePickerDialog();
        dialog.setArguments(bundle);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Date date = (Date) getArguments().getSerializable(ARG_DATE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        View view = inflater.inflate(R.layout.dialog_date, container, false);
        mDatePicker = view.findViewById(R.id.dialog_date_date_picker);
        mDatePicker.init(year, month, day, null);

        mConfirmTextView = view.findViewById(R.id.dialog_confirm_text_view);
        mConfirmTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int year = mDatePicker.getYear();
                int month = mDatePicker.getMonth();
                int day = mDatePicker.getDayOfMonth();
                Date date = new GregorianCalendar(year, month, day).getTime();
                setResult(Activity.RESULT_OK, date);
            }
        });

        return view;
    }

    /**
     * return data to target fragment
     */
    private void setResult(int resultCode, Date date) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DATE, date);

        if (null == getTargetFragment()) {
            getActivity().setResult(resultCode, intent);
            getActivity().finish();
        } else {
            getTargetFragment()
                    .onActivityResult(getTargetRequestCode(), resultCode, intent);
            dismiss();
        }
    }

}
