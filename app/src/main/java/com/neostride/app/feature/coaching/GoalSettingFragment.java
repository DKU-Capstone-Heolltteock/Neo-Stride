package com.neostride.app.feature.coaching;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.neostride.app.R;

import java.util.ArrayList;
import java.util.List;

public class GoalSettingFragment extends Fragment {

    private int selectedPeriodWeeks = 4;
    private boolean isCustomPeriod = false;
    private List<String> selectedDays = new ArrayList<>();
    private float selectedDistanceKm = 3f;
    private boolean isCustomDistance = false;
    private int selectedPaceSecPerKm = 360;
    private boolean isCustomPace = false;

    private TextView btnP1m, btnP3m, btnP6m, btnP1y;
    private TextView[] periodButtons;
    private int[] periodWeeks = {4, 12, 24, 52};

    private TextView btnSun, btnMon, btnTue, btnWed, btnThu, btnFri, btnSat;
    private TextView[] dayButtons;
    private String[] dayKeys = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};

    private TextView btnD3, btnD5, btnD10, btnD20, btnD40;
    private TextView[] distButtons;
    private float[] distValues = {3f, 5f, 10f, 20f, 40f};

    private EditText etCustomWeeks, etCustomDistance, etCustomPaceMin, etCustomPaceSec;
    private LinearLayout layoutCustomPeriod, layoutCustomDistance, layoutCustomPace;
    private SeekBar seekbarPace;
    private TextView tvPaceValue, tvDayWarning;

    private OnGoalSavedListener onGoalSavedListener;

    public interface OnGoalSavedListener {
        void onGoalSaved(GoalStorage.GoalInputData goalData);
    }

    public void setOnGoalSavedListener(OnGoalSavedListener listener) {
        this.onGoalSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goal_setting, container, false);

        btnP1m = view.findViewById(R.id.btn_period_1m);
        btnP3m = view.findViewById(R.id.btn_period_3m);
        btnP6m = view.findViewById(R.id.btn_period_6m);
        btnP1y = view.findViewById(R.id.btn_period_1y);
        periodButtons = new TextView[]{btnP1m, btnP3m, btnP6m, btnP1y};

        for (int i = 0; i < periodButtons.length; i++) {
            int weeks = periodWeeks[i];
            periodButtons[i].setOnClickListener(v -> {
                selectedPeriodWeeks = weeks;
                isCustomPeriod = false;
                etCustomWeeks.setText("");
                updatePeriodUI();
            });
        }

        etCustomWeeks = view.findViewById(R.id.et_custom_weeks);
        layoutCustomPeriod = view.findViewById(R.id.layout_custom_period);
        etCustomWeeks.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    isCustomPeriod = true;
                    try { selectedPeriodWeeks = Integer.parseInt(s.toString()); } catch (Exception e) {}
                } else { isCustomPeriod = false; }
                updatePeriodUI();
            }
        });

        btnSun = view.findViewById(R.id.btn_day_sun);
        btnMon = view.findViewById(R.id.btn_day_mon);
        btnTue = view.findViewById(R.id.btn_day_tue);
        btnWed = view.findViewById(R.id.btn_day_wed);
        btnThu = view.findViewById(R.id.btn_day_thu);
        btnFri = view.findViewById(R.id.btn_day_fri);
        btnSat = view.findViewById(R.id.btn_day_sat);
        dayButtons = new TextView[]{btnSun, btnMon, btnTue, btnWed, btnThu, btnFri, btnSat};
        tvDayWarning = view.findViewById(R.id.tv_day_warning);

        for (int i = 0; i < dayButtons.length; i++) {
            int idx = i;
            dayButtons[i].setOnClickListener(v -> {
                String key = dayKeys[idx];
                if (selectedDays.contains(key)) selectedDays.remove(key);
                else selectedDays.add(key);
                updateDayUI();
                tvDayWarning.setVisibility(View.GONE);
            });
        }

        btnD3 = view.findViewById(R.id.btn_dist_3);
        btnD5 = view.findViewById(R.id.btn_dist_5);
        btnD10 = view.findViewById(R.id.btn_dist_10);
        btnD20 = view.findViewById(R.id.btn_dist_20);
        btnD40 = view.findViewById(R.id.btn_dist_40);
        distButtons = new TextView[]{btnD3, btnD5, btnD10, btnD20, btnD40};

        for (int i = 0; i < distButtons.length; i++) {
            float dist = distValues[i];
            distButtons[i].setOnClickListener(v -> {
                selectedDistanceKm = dist;
                isCustomDistance = false;
                etCustomDistance.setText("");
                updateDistUI();
            });
        }

        etCustomDistance = view.findViewById(R.id.et_custom_distance);
        layoutCustomDistance = view.findViewById(R.id.layout_custom_distance);
        etCustomDistance.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    isCustomDistance = true;
                    try { selectedDistanceKm = Float.parseFloat(s.toString()); } catch (Exception e) {}
                } else { isCustomDistance = false; }
                updateDistUI();
            }
        });

        seekbarPace = view.findViewById(R.id.seekbar_pace);
        tvPaceValue = view.findViewById(R.id.tv_pace_value);
        layoutCustomPace = view.findViewById(R.id.layout_custom_pace);
        etCustomPaceMin = view.findViewById(R.id.et_custom_pace_min);
        etCustomPaceSec = view.findViewById(R.id.et_custom_pace_sec);

        seekbarPace.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    isCustomPace = false;
                    etCustomPaceMin.setText("");
                    etCustomPaceSec.setText("");
                    updatePaceUI();
                }
                selectedPaceSecPerKm = progress;
                int min = progress / 60;
                int sec = progress % 60;
                tvPaceValue.setText(String.format("%d:%02d/km", min, sec));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        SimpleWatcher paceWatcher = new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String minStr = etCustomPaceMin.getText().toString().trim();
                String secStr = etCustomPaceSec.getText().toString().trim();
                if (minStr.length() > 0 || secStr.length() > 0) {
                    isCustomPace = true;
                    int min = minStr.isEmpty() ? 0 : Integer.parseInt(minStr);
                    int sec = secStr.isEmpty() ? 0 : Integer.parseInt(secStr);
                    selectedPaceSecPerKm = min * 60 + sec;
                    tvPaceValue.setText(String.format("%d:%02d/km", min, sec));
                } else { isCustomPace = false; }
                updatePaceUI();
            }
        };
        etCustomPaceMin.addTextChangedListener(paceWatcher);
        etCustomPaceSec.addTextChangedListener(paceWatcher);

        // Confirm
        CardView btnConfirm = view.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            // 러닝 데이 필수 체크
            if (selectedDays.isEmpty()) {
                tvDayWarning.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "러닝 데이를 1일 이상 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            GoalStorage.GoalInputData goalData = new GoalStorage.GoalInputData();
            goalData.durationWeeks = selectedPeriodWeeks;
            goalData.runningDays = new ArrayList<>(selectedDays);
            goalData.distanceKm = selectedDistanceKm;
            goalData.paceSecPerKm = selectedPaceSecPerKm;

            if (onGoalSavedListener != null) onGoalSavedListener.onGoalSaved(goalData);
            getParentFragmentManager().popBackStack();
        });

        updatePeriodUI();
        updateDayUI();
        updateDistUI();
        updatePaceUI();

        return view;
    }

    private void updatePeriodUI() {
        for (int i = 0; i < periodButtons.length; i++) {
            if (!isCustomPeriod && periodWeeks[i] == selectedPeriodWeeks) {
                periodButtons[i].setBackgroundResource(R.drawable.btn_period_selected);
                periodButtons[i].setTextColor(0xFF000000);
                periodButtons[i].setAlpha(1f);
            } else {
                periodButtons[i].setBackgroundResource(R.drawable.btn_period_unselected);
                periodButtons[i].setTextColor(0xFFAAAAAA);
                periodButtons[i].setAlpha(isCustomPeriod ? 0.4f : 1f);
            }
        }
        if (isCustomPeriod) {
            layoutCustomPeriod.setBackgroundResource(R.drawable.btn_period_selected);
            etCustomWeeks.setTextColor(0xFF000000);
        } else {
            layoutCustomPeriod.setBackgroundResource(R.drawable.btn_period_unselected);
            etCustomWeeks.setTextColor(0xFFFFFFFF);
        }
    }

    private void updateDayUI() {
        for (int i = 0; i < dayButtons.length; i++) {
            if (selectedDays.contains(dayKeys[i])) {
                dayButtons[i].setBackgroundResource(R.drawable.btn_period_selected);
                dayButtons[i].setTextColor(0xFF000000);
            } else {
                dayButtons[i].setBackgroundResource(R.drawable.btn_period_unselected);
                dayButtons[i].setTextColor(0xFFAAAAAA);
            }
        }
    }

    private void updateDistUI() {
        for (int i = 0; i < distButtons.length; i++) {
            if (!isCustomDistance && distValues[i] == selectedDistanceKm) {
                distButtons[i].setBackgroundResource(R.drawable.btn_period_selected);
                distButtons[i].setTextColor(0xFF000000);
                distButtons[i].setAlpha(1f);
            } else {
                distButtons[i].setBackgroundResource(R.drawable.btn_period_unselected);
                distButtons[i].setTextColor(0xFFAAAAAA);
                distButtons[i].setAlpha(isCustomDistance ? 0.4f : 1f);
            }
        }
        if (isCustomDistance) {
            layoutCustomDistance.setBackgroundResource(R.drawable.btn_period_selected);
            etCustomDistance.setTextColor(0xFF000000);
        } else {
            layoutCustomDistance.setBackgroundResource(R.drawable.btn_period_unselected);
            etCustomDistance.setTextColor(0xFFFFFFFF);
        }
    }

    private void updatePaceUI() {
        if (isCustomPace) {
            seekbarPace.setEnabled(false);
            seekbarPace.setAlpha(0.3f);
            tvPaceValue.setTextColor(0xFFCCFF00);
            layoutCustomPace.setBackgroundResource(R.drawable.btn_period_selected);
            etCustomPaceMin.setTextColor(0xFF000000);
            etCustomPaceSec.setTextColor(0xFF000000);
        } else {
            seekbarPace.setEnabled(true);
            seekbarPace.setAlpha(1f);
            tvPaceValue.setTextColor(0xFFCCFF00);
            layoutCustomPace.setBackgroundResource(R.drawable.btn_period_unselected);
            etCustomPaceMin.setTextColor(0xFFFFFFFF);
            etCustomPaceSec.setTextColor(0xFFFFFFFF);
        }
    }

    // TextWatcher 간소화
    abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}