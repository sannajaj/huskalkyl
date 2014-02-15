package se.caleklint.kopahus;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity implements OnSeekBarChangeListener {

	private static final int MIN_CASH_PERCENT = 15;
	private static final float MORTGAGE_PERCENT_FEE = 0.02f;
	private static final int MORTGAGE_FEE = 375;
	private static final int DEED_FEE = 875;
	private static final float DEED_PERCENT_FEE = 0.015f;

	private static final String EMPTY_STRING = "";

	private static final String PRICE_KEY = "price_key";
	private static final String MORTGAGE_KEY = "mortgage_key";
	private static final String PERCENT_KEY = "percent_key";

	private EditText mPriceEdit;
	private EditText mMortgageEdit;
	private TextView mCashLabel;
	private SeekBar mCashPercent;	
	private TextView mResultText;

	private NumberTextWatcher mPriceWatcher;
	private NumberTextWatcher mMortgageWatcher;
	private DecimalFormat mNumberFormat;
	private String mSeparator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		mPriceEdit = (EditText) findViewById(R.id.price);
		mMortgageEdit = (EditText) findViewById(R.id.pant);
		mCashLabel = (TextView) findViewById(R.id.cash_label);
		mCashPercent = (SeekBar) findViewById(R.id.cash);
		mResultText = (TextView) findViewById(R.id.result);

		mPriceWatcher = new NumberTextWatcher(mPriceEdit);
		mMortgageWatcher = new NumberTextWatcher(mMortgageEdit);
		mNumberFormat = (DecimalFormat) NumberFormat.getInstance();
		mSeparator = String.valueOf(mNumberFormat.getDecimalFormatSymbols().getGroupingSeparator());

		mCashLabel.setText(getString(R.string.cash, MIN_CASH_PERCENT));
		mCashPercent.setOnSeekBarChangeListener(this);
		mCashPercent.setProgress(MIN_CASH_PERCENT);

		if (savedInstanceState != null) {
			String priceString = savedInstanceState.getString(PRICE_KEY);
			if (priceString != null) {
				mPriceEdit.setText(priceString);
			}
			String mortgageString = savedInstanceState.getString(MORTGAGE_KEY);
			if (mortgageString != null) {
				mMortgageEdit.setText(mortgageString);
			}			
			mCashPercent.setProgress(savedInstanceState.getInt(PERCENT_KEY, MIN_CASH_PERCENT));
		}

		calculate();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mPriceEdit.addTextChangedListener(mPriceWatcher);
		mMortgageEdit.addTextChangedListener(mMortgageWatcher);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPriceEdit.removeTextChangedListener(mPriceWatcher);
		mMortgageEdit.removeTextChangedListener(mMortgageWatcher);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(PRICE_KEY, mPriceEdit.getText().toString());
		outState.putString(MORTGAGE_KEY, mMortgageEdit.getText().toString());
		outState.putInt(PERCENT_KEY, mCashPercent.getProgress());
		super.onSaveInstanceState(outState);
	}

	private void calculate() {
		try {
			long price = 0; 
			if (mPriceEdit.getText().length() > 0) {
				String priceString = mPriceEdit.getText().toString().replace(mSeparator, EMPTY_STRING);
				price = Long.parseLong(priceString);
			}
			long cash = price * mCashPercent.getProgress() / 100;
			long loan = price - cash;
			long mortgage = 0;
			if (mMortgageEdit.getText().length() > 0) {
				String mortgageString = mMortgageEdit.getText().toString().replace(mSeparator, EMPTY_STRING);
				mortgage = Long.parseLong(mortgageString);
			}
			long leftToMortgage = loan - mortgage;
			if (leftToMortgage <= 0) {
				leftToMortgage = 0;
			} else {
				leftToMortgage = Math.round(leftToMortgage * MORTGAGE_PERCENT_FEE) + MORTGAGE_FEE;
			}
			long deed = Math.round(price * DEED_PERCENT_FEE) + DEED_FEE;
			long total = cash + deed + leftToMortgage;
			String cashString = mNumberFormat.format(cash);
			String mortgageString = mNumberFormat.format(leftToMortgage);
			String deedString = mNumberFormat.format(deed);
			String totalString = mNumberFormat.format(total);
			mResultText.setText(getString(R.string.result_format, cashString, 
					mortgageString, deedString, totalString));
		} catch (NumberFormatException e) {
			Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
		if (fromUser && progress < MIN_CASH_PERCENT) {
			seekbar.setProgress(MIN_CASH_PERCENT);
		} else {		
			mCashLabel.setText(getString(R.string.cash, progress));
			calculate();
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {}

	private class NumberTextWatcher implements TextWatcher {

		private EditText mWatchedEditor;

		public NumberTextWatcher(EditText editText) {
			mWatchedEditor = editText;
		}

		@Override
		public void afterTextChanged(Editable s) {
			mWatchedEditor.removeTextChangedListener(this);
			try {
				int inilen, endlen;
				inilen = mWatchedEditor.getText().length();

				String v = s.toString().replace(mSeparator, EMPTY_STRING);
				Number n = mNumberFormat.parse(v);
				int cp = mWatchedEditor.getSelectionStart();
				mWatchedEditor.setText(mNumberFormat.format(n));
				endlen = mWatchedEditor.getText().length();
				int sel = (cp + (endlen - inilen));
				if (sel > 0 && sel <= mWatchedEditor.getText().length()) {
					mWatchedEditor.setSelection(sel);
				} else {
					// place cursor at the end?
					mWatchedEditor.setSelection(mWatchedEditor.getText().length() - 1);
				}
			} catch (NumberFormatException nfe) {
				// Will be done in calculate instead
			} catch (ParseException e) {
				// do nothing?
			}
			mWatchedEditor.addTextChangedListener(this);
			calculate();
		}

		@Override
		public void beforeTextChanged(CharSequence text, int start, int length, int after) {}

		@Override
		public void onTextChanged(CharSequence text, int start, int length, int after) {}

	}    
}
