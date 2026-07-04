package com.neostride.app.feature.main.record;

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
import com.neostride.app.feature.main.running.model.RunningRecordResponse;


//  AI 코칭 달성도 꺾은선 차트 커스텀 뷰
//  <p>
//  - AI 코칭 기록별 실제 페이스를 형광 초록 꺾은선으로 그린다.
//  - 목표 페이스를 반투명 점선으로 그려 실적과 비교한다.
//  - 각 포인트에 페이스 라벨·날짜·목표 거리를 표시하며, 최종 목표 달성 시 원을 채운다.

public class AiLineChartView extends View {
    // ── 데이터 ──
    private List<RunningRecordResponse> records = new ArrayList<>();
    private List<Float> dailyTargetDistances = new ArrayList<>();
    private float targetPaceValue = 5.5f;     // 목표 페이스 (분/km)
    private float finalGoalDistance = 0f;     // 최종 목표 거리 (km)

    // ── 페인트 ──
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

        // 실제 데이터와 목표 페이스를 모두 고려해 범위를 동적으로 계산
        float minPace = targetPaceValue;
        float maxPace = targetPaceValue;
        for (RunningRecordResponse res : records) {
            float p = res.getPace() < 60 ? res.getPace() : res.getPace() / 60f;
            if (p < minPace) minPace = p;
            if (p > maxPace) maxPace = p;
        }
        // 여백 추가 (위아래 10%)
        float margin = Math.max((maxPace - minPace) * 0.15f, 0.5f);
        minPace = Math.max(0f, minPace - margin);
        maxPace = maxPace + margin;
        float paceRange = (maxPace - minPace) > 0 ? (maxPace - minPace) : 1f;

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

                // pace < 60이면 구버전(분 단위), >= 60이면 신버전(초 단위) → 분/km로 통일
                float currPace = res.getPace() < 60 ? res.getPace() : res.getPace() / 60f;
                float y = (height - paddingBottom) - ((maxPace - currPace) / paceRange * chartHeight);

                // --- 목표 달성 여부에 따른 원 그리기 ---
                // 당일 목표 거리 + 목표 페이스 둘 다 달성해야 꽉찬 원
                float dailyTarget = (dailyTargetDistances != null && i < dailyTargetDistances.size())
                        ? dailyTargetDistances.get(i) : 0f;
                boolean distanceAchieved = dailyTarget > 0 && res.getDistance() >= dailyTarget;
                boolean paceAchieved = currPace <= targetPaceValue; // 페이스는 낮을수록(빠를수록) 달성
                if (distanceAchieved && paceAchieved) {
                    pointPaint.setStyle(Paint.Style.FILL);
                } else {
                    pointPaint.setStyle(Paint.Style.STROKE);
                }

                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                canvas.drawCircle(x, y, 12f, pointPaint);

                // 데이터 라벨 정렬 결정: 양쪽 끝 점은 화면 밖으로 잘리지 않도록 LEFT/RIGHT 정렬
                Paint.Align labelAlign;
                if (records.size() == 1) {
                    labelAlign = Paint.Align.CENTER;
                } else if (i == 0) {
                    labelAlign = Paint.Align.LEFT;
                } else if (i == records.size() - 1) {
                    labelAlign = Paint.Align.RIGHT;
                } else {
                    labelAlign = Paint.Align.CENTER;
                }

                textPaint.setColor(Color.WHITE);
                textPaint.setTextAlign(labelAlign);

                int paceSeconds = res.getPace() < 60 ? (int)(res.getPace() * 60) : (int) res.getPace();
                int pMin = paceSeconds / 60;
                int pSec = paceSeconds % 60;
                String formattedPace = String.format(Locale.KOREA, "%d:%02d", pMin, pSec);
                canvas.drawText(formattedPace, x, y - 30f, textPaint);

                // 하단 날짜
                String dateStr = res.getCreatedAt().substring(5, 10).replace("-", "/");
                canvas.drawText(dateStr, x, height - 40f, textPaint);

                // 하단 거리 표기 (3.20km 형식)
                if (dailyTargetDistances != null && i < dailyTargetDistances.size()) {
                    String distStr = String.format(Locale.KOREA, "%.2fkm", dailyTargetDistances.get(i));
                    canvas.drawText(distStr, x, height - 5f, textPaint);
                }
            }
            canvas.drawPath(path, linePaint);
        }
    }
}