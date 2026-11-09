package com.example.studymate.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.ai.AiModel;
import com.example.studymate.ai.ChatMessage;
import com.example.studymate.ai.ContentExtractor;
import com.example.studymate.databinding.ActivityAiTutorBinding;
import com.example.studymate.databinding.ItemChatMessageBinding;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.FileUtil;
import com.example.studymate.util.Prefs;
import com.example.studymate.viewmodel.AiViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AiTutorActivity extends BaseActivity {
    private static final int M_KEY = 1, M_CLEAR = 2;

    private ActivityAiTutorBinding b;
    private AiViewModel vm;
    private ChatAdapter adapter;

    private final ActivityResultLauncher<String[]> documentPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                FileUtil.takePersist(this, uri);
                AppExecutors.io(() -> {
                    try {
                        ContentExtractor.ExtractedContent ext = ContentExtractor.extract(this, uri, null);
                        AppExecutors.main(() -> vm.setAttachedContent(ext));
                    } catch (Exception e) {
                        AppExecutors.main(() -> UiUtil.toast(this, "Could not extract file: " + e.getMessage()));
                    }
                });
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAiTutorBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "StudyMate Bot", true);

        vm = new ViewModelProvider(this).get(AiViewModel.class);

        adapter = new ChatAdapter();
        b.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        b.rvMessages.setAdapter(adapter);

        setupModelSpinner();
        checkConsentAndNetwork();

        vm.getMessages().observe(this, list -> {
            adapter.submitList(list);
            if (!list.isEmpty()) {
                b.rvMessages.post(() -> b.rvMessages.smoothScrollToPosition(list.size() - 1));
            }
        });

        vm.getIsLoading().observe(this, loading -> {
            b.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            b.btnSend.setEnabled(!Boolean.TRUE.equals(loading));
            b.chipSummarize.setEnabled(!Boolean.TRUE.equals(loading));
            b.chipExplain.setEnabled(!Boolean.TRUE.equals(loading));
            b.chipQuiz.setEnabled(!Boolean.TRUE.equals(loading));
        });

        vm.getErrorMessage().observe(this, err -> {
            if (err != null && !err.isEmpty()) {
                UiUtil.toast(this, err);
            }
        });

        vm.getAttachedContent().observe(this, content -> {
            if (content != null) {
                b.cardAttached.setVisibility(View.VISIBLE);
                b.tvAttachedName.setText("Attached: " + content.fileName);
            } else {
                b.cardAttached.setVisibility(View.GONE);
            }
        });

        vm.getGeneratedQuiz().observe(this, questions -> {
            if (questions != null && !questions.isEmpty()) {
                QuizActivity.currentQuiz = questions;
                startActivity(new Intent(this, QuizActivity.class));
                vm.clearQuiz();
            }
        });

        b.btnSend.setOnClickListener(v -> doSend());
        b.btnAttach.setOnClickListener(v -> documentPicker.launch(new String[]{
                "application/pdf",
                Openers.MIME_PPTX,
                Openers.MIME_DOCX,
                "image/*",
                "application/octet-stream"
        }));
        b.btnRemoveAttachment.setOnClickListener(v -> vm.setAttachedContent(null));

        b.chipSummarize.setOnClickListener(v -> vm.summarize());

        b.chipExplain.setOnClickListener(v -> {
            b.etInput.setText("Explain the main concepts of this material in detail with examples:");
            b.etInput.setSelection(b.etInput.getText().length());
        });

        b.chipQuiz.setOnClickListener(v -> vm.generateQuiz(5, "Medium"));

        Intent intent = getIntent();
        String uriStr = intent.getStringExtra("uri");
        String fileExtra = intent.getStringExtra("file");
        String name = intent.getStringExtra("name");

        if (uriStr != null || fileExtra != null) {
            Uri fileUri = uriStr != null ? Uri.parse(uriStr) : Uri.parse(fileExtra);
            AppExecutors.io(() -> {
                try {
                    ContentExtractor.ExtractedContent ext = ContentExtractor.extract(this, fileUri, name);
                    AppExecutors.main(() -> vm.setAttachedContent(ext));
                } catch (Exception e) {
                    AppExecutors.main(() -> UiUtil.toast(this, "Could not extract file content: " + e.getMessage()));
                }
            });
        }
    }

    private void checkConsentAndNetwork() {
        if (!Prefs.aiConsent(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("StudyMate Bot Online Notice")
                    .setMessage("StudyMate Bot connects online to analyze study materials, summarize text, and generate practice quizzes. Your prompt/file content will be sent securely to AI services.\n\nDo you wish to enable AI features?")
                    .setPositiveButton("Enable AI", (d, w) -> Prefs.setAiConsent(this, true))
                    .setNegativeButton("Disable AI", (d, w) -> {
                        Prefs.setAiEnabled(this, false);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private void setupModelSpinner() {
        List<AiModel> models = AiModel.DEFAULT_MODELS;
        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        AiModel cur = vm.getSelectedModel().getValue();
        String curId = cur != null ? cur.id : Prefs.selectedAiModelId(this);

        for (int i = 0; i < models.size(); i++) {
            AiModel m = models.get(i);
            names.add(m.name + (m.isFree ? " (Free)" : ""));
            if (m.id.equals(curId)) selectedIndex = i;
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spinnerModel.setAdapter(spinnerAdapter);
        b.spinnerModel.setSelection(selectedIndex);

        b.spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < models.size()) {
                    vm.selectModel(models.get(position));
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void doSend() {
        String text = b.etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        b.etInput.setText("");
        vm.sendMessage(text);
    }

    private void showKeyDialog() {
        FormDialog f = new FormDialog(this);
        EditText input = f.text("API Key", Prefs.openRouterKey(this));
        input.setHint("Paste sk-or-v1-... key here");
        f.show("OpenRouter API Key", "Save", () -> {
            String key = FormDialog.str(input).trim();
            Prefs.setOpenRouterKey(this, key);
            UiUtil.toast(this, key.isEmpty() ? "Key cleared" : "API Key saved");
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        addMenu(menu, M_KEY, "API Key Settings");
        addMenu(menu, M_CLEAR, "Clear Chat");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_KEY) {
            showKeyDialog();
            return true;
        } else if (item.getItemId() == M_CLEAR) {
            vm.clearChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        private final List<ChatMessage> data = new ArrayList<>();

        public void submitList(List<ChatMessage> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ChatMessage msg = data.get(position);
            holder.b.tvContent.setText(msg.content);

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.b.msgCard.getLayoutParams();

            if (msg.isUser()) {
                lp.gravity = Gravity.END;
                holder.b.msgContainer.setGravity(Gravity.END);
                holder.b.msgCard.setCardBackgroundColor(0xFF3F51B5);
                holder.b.tvContent.setTextColor(0xFFFFFFFF);
                holder.b.tvModelBadge.setVisibility(View.GONE);
            } else {
                lp.gravity = Gravity.START;
                holder.b.msgContainer.setGravity(Gravity.START);
                holder.b.msgCard.setCardBackgroundColor(UiUtil.isNight(holder.itemView.getContext()) ? 0xFF2C2C2C : 0xFFF0F0F0);
                holder.b.tvContent.setTextColor(UiUtil.isNight(holder.itemView.getContext()) ? 0xFFFFFFFF : 0xFF000000);

                if (msg.modelName != null && !msg.modelName.isEmpty()) {
                    holder.b.tvModelBadge.setText(msg.modelName);
                    holder.b.tvModelBadge.setTextColor(0xFF3F51B5);
                    holder.b.tvModelBadge.setVisibility(View.VISIBLE);
                } else {
                    holder.b.tvModelBadge.setVisibility(View.GONE);
                }
            }
            holder.b.msgCard.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ItemChatMessageBinding b;
            VH(ItemChatMessageBinding binding) {
                super(binding.getRoot());
                this.b = binding;
            }
        }
    }
}
