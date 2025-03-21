package com.example.smartfoodinventorytracker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.UUID;

public class AddManualProductDialogFragment extends DialogFragment {

    private EditText etName, etBrand, etExpiryDate;
    private Button btnDone, btnCancel;
    private String selectedExpiryDate;
    private Button btnExpiryDate;

    public interface ManualProductListener {
        void onProductAdded(Product product);
    }

    private ManualProductListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ManualProductListener) {
            listener = (ManualProductListener) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement ManualProductListener");
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_manual_product, null);

        etName = view.findViewById(R.id.et_product_name);
        etBrand = view.findViewById(R.id.et_product_brand);
        btnExpiryDate = view.findViewById(R.id.btn_pick_expiry_date);
        btnDone = view.findViewById(R.id.btn_done);
        btnCancel = view.findViewById(R.id.btn_cancel);

        btnExpiryDate.setOnClickListener(v -> showDatePickerDialog());

        btnDone.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String brand = etBrand.getText().toString();

            if (name.isEmpty() || brand.isEmpty() || selectedExpiryDate == null) {
                Toast.makeText(getContext(), "All fields must be filled!", Toast.LENGTH_SHORT).show();
                return;
            }

            String barcode = UUID.nameUUIDFromBytes((name + brand + selectedExpiryDate).getBytes()).toString();
            Product product = new Product(barcode, name, brand);
            product.setExpiryDate(selectedExpiryDate);

            listener.onProductAdded(product);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view).setTitle("Add Product Manually");
        return builder.create();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance(); // Get current date
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Convert selected date to LocalDate
                    LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);

                    // Get today's date
                    LocalDate today = LocalDate.now();

                    // ✅ Check if expiry date is before today
                    if (selectedDate.isBefore(today)) {
                        Toast.makeText(getContext(), "Expiry date cannot be before today's date!", Toast.LENGTH_SHORT).show();
                    } else {
                        selectedExpiryDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        btnExpiryDate.setText("Expiry Date: " + selectedExpiryDate);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // ✅ Prevent selecting past dates
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();
    }

}
