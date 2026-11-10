package com.example.studymate.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class DocumentToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_document_tools);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        if (tb != null) {
            tb.setNavigationOnClickListener(v -> finish());
        }

        List<ToolItem> tools = new ArrayList<>();
        tools.add(new ToolItem("PDF Reader", "Open, zoom, bookmark", R.drawable.ic_pdf, ContextCompat.getColor(this, R.color.tool_red), PdfLibraryActivity.class));
        tools.add(new ToolItem("Image → PDF", "Scan or pick photos", R.drawable.ic_image, ContextCompat.getColor(this, R.color.tool_green), ImageToPdfActivity.class));
        tools.add(new ToolItem("Course Files", "Slides and past papers", R.drawable.ic_folder, ContextCompat.getColor(this, R.color.tool_teal), CourseFilesActivity.class));
        tools.add(new ToolItem("Merge PDFs", "Combine PDFs into one", R.drawable.ic_merge, ContextCompat.getColor(this, R.color.tool_purple), MergePdfActivity.class));
        tools.add(new ToolItem("StudyMate Bot", "Multi-model AI assistant", R.drawable.ic_notification, ContextCompat.getColor(this, R.color.tool_purple), AiTutorActivity.class));

        RecyclerView rv = findViewById(R.id.rvTools);
        int widthDp = (int) (getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density);
        rv.setLayoutManager(new GridLayoutManager(this, Math.max(2, widthDp / 170)));
        rv.setAdapter(new ToolAdapter(tools));
    }

    private class ToolAdapter extends RecyclerView.Adapter<ToolAdapter.VH> {
        private final List<ToolItem> data;
        ToolAdapter(List<ToolItem> d) { data = d; }

        class VH extends RecyclerView.ViewHolder {
            FrameLayout iconWrap;
            ImageView icon;
            TextView title, sub;
            VH(View v) {
                super(v);
                iconWrap = v.findViewById(R.id.iconWrap);
                icon = v.findViewById(R.id.ivIcon);
                title = v.findViewById(R.id.tvTitle);
                sub = v.findViewById(R.id.tvSubtitle);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_tool_card, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ToolItem it = data.get(pos);
            h.title.setText(it.title);
            h.sub.setText(it.subtitle);
            h.icon.setImageResource(it.iconRes);
            h.icon.setColorFilter(it.color);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(ColorUtils.setAlphaComponent(it.color, 40));
            h.iconWrap.setBackground(bg);

            h.itemView.setOnClickListener(v -> {
                if (it.target != null) {
                    startActivity(new Intent(DocumentToolsActivity.this, it.target));
                }
            });
        }

        @Override public int getItemCount() { return data.size(); }
    }
}
