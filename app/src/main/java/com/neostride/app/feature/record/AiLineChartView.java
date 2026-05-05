package com.neostride.app.feature.record;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.neostride.app.feature.running.model.RunningRecordResponse;

public class AiLineChartView extends View {
    private List<RunningRecordResponse> records = new ArrayList<>();
    private List<Float> dailyTargetDistances = new ArrayList<>();
    private float targetPaceValue = 5.5f;
    private float finalGoalDistance = 0f;

    private Paint linePaint, pointPaint, textPaint, targetLinePaint;

    public AiLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        linePaint = new Paint();
        linePaint.setColor(0xFFCCFF00); // 형광 초록
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        pointPaint = new Paint();
        pointPaint.setColor(0xFFCCFF00);
        pointPaint.setStrokeWidth(4f);
        pointPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(26f);
        textPaint.setAntiAlias(true);

        targetLinePaint = new Paint();
        targetLinePaint.setColor(0x88CCFF00); // 투명도 있는 형광 초록
        targetLinePaint.setStrokeWidth(3f);
        targetLinePaint.setStyle(Paint.Style.STROKE);
        targetLinePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
    }

    public void setTargetPace(float pace) {
        this.targetPaceValue = pace;
        invalidate();
    }

    public void setFinalGoalDistance(float distance) {
        this.finalGoalDistance = distance;
        invalidate();
    }

    public void setData(List<RunningRecordResponse> data, List<Float> targets) {
        this.records = data;
        this.dailyTargetDistances = targets;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (records == null) return;

        float width = getWidth();
        float height = getHeight();

        // 🌟 공간 효율화: 왼쪽 패딩을 대폭 줄여서 여백 제거
        float paddingLeft = 20f;
        float paddingRight = 20f;
        float paddingBottom = 80f;
        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingBottom - 60f;

        float minPace = 3.0f;
        float maxPace = 10.0f;
        float paceRange = maxPace - minPace;

        // --- 1. 목표 점선 및 우측 라벨 그리기 ---
        float targetY = (height - paddingBottom) - ((maxPace - targetPaceValue) / paceRange * chartHeight);
        canvas.drawLine(paddingLeft, targetY, width - paddingRight, targetY, targetLinePaint);

        // 🌟 점선 우측 끝에 목표 페이스 표시 (형광 초록)
        textPaint.setColor(0xFFCCFF00);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        int tMin = (int) targetPaceValue;
        int tSec = (int) Math.round((targetPaceValue - tMin) * 60);
        canvas.drawText(String.format(Locale.KOREA, "%d:%02d", tMin, tSec), width - paddingRight, targetY - 10f, textPaint);

        if (records.size() > 0) {
            float xStep = (records.size() > 1) ? chartWidth / (records.size() - 1) : 0;
            Path path = new Path();

            for (int i = 0; i < records.size(); i++) {
                RunningRecordResponse res = records.get(i);
                float x = (records.size() > 1) ? paddingLeft + (xStep * i) : paddingLeft + (chartWidth / 2);

                float currPace = (float) res.getPace();
                if (currPace < minPace) currPace = minPace;
                if (currPace > maxPace) currPace = maxPace;
                float y = (height - paddingBottom) - ((maxPace - currPace) / paceRange * chartHeight);

                // --- 목표 달성 여부에 따른 원 그리기 ---
                if (finalGoalDistance > 0 && res.getDistance() >= finalGoalDistance) {
                    pointPaint.setStyle(Paint.Style.FILL);
                } else {
                    pointPaint.setStyle(Paint.Style.STROKE);
                }

                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                canvas.drawCircle(x, y, 12f, pointPaint);

                // 데이터 라벨을 5:30 형식으로 변경
                textPaint.setColor(Color.WHITE);
                textPaint.setTextAlign(Paint.Align.CENTER);

                // 1. 소수점 페이스를 분(min)과 초(sec)로 분리합니다.
                int pMin = (int) res.getPace();
                int pSec = (int) Math.round((res.getPace() - pMin) * 60);

                // 2. 분:초 형식으로 문자열을 조립합니다.
                String formattedPace = String.format(Locale.KOREA, "%d:%02d", pMin, pSec);

                // 3. 화면에 출력합니다.
                canvas.drawText(formattedPace, x, y - 30f, textPaint);

                // 하단 날짜
                String dateStr = res.getCreatedAt().substring(5, 10).replace("-", "/");
                canvas.drawText(dateStr, x, height - 40f, textPaint);

                // 하단 거리 표기 수정 (3.20km 형식)
                if (dailyTargetDistances != null && i < dailyTargetDistances.size()) {
                    String distStr = String.format(Locale.KOREA, "%.2fkm", dailyTargetDistances.get(i));
                    canvas.drawText(distStr, x, height - 5f, textPaint);
                }
            }
            canvas.drawPath(path, linePaint);
        }
    }
}