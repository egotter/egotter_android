package com.egotter.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.egotter.MainActivity;
import com.egotter.R;

public class SignOutConfirmationDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.signOutConfirmationMessage))
                .setPositiveButton(R.string.signOutConfirmationOk, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        FragmentActivity activity = getActivity();
                        if (activity != null) {
                            ((MainActivity) activity).doSignOut();
                        }
                    }
                })
                .setNegativeButton(R.string.signOutConfirmationCancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }
}
