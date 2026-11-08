package com.example.studymate.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.LruCache;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studymate.R;
import com.example.studymate.data.entity.Contact;
import com.example.studymate.databinding.ActivityListBinding;
import com.example.studymate.databinding.ItemContactBinding;
import com.example.studymate.docs.ImageUtil;
import com.example.studymate.firebase.ContactSync;
import com.example.studymate.firebase.FirebaseSync;
import com.example.studymate.util.AppExecutors;
import com.example.studymate.util.ContactPhotos;
import com.example.studymate.viewmodel.ContactViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.RelativeCornerSize;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Teachers and classmates: stored in Room (SQLite) and synced to Firestore when signed in. */
public class ContactsActivity extends BaseActivity {
    private static final int M_SEARCH = 100, M_SYNC = 101;
    private static final String[] TYPE_NAMES = {"Teacher", "Classmate"};

    private ActivityListBinding b;
    private ContactViewModel vm;
    private ContactAdapter adapter;
    private final List<Contact> items = new ArrayList<>();
    private final LruCache<String, Bitmap> photoCache = new LruCache<>(60);

    // state of the add/edit dialog
    private Uri pendingPhoto;
    private boolean removePhoto;
    private ShapeableImageView formAvatar;

    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                try {
                    Bitmap bm = ImageUtil.decode(this, uri, 256);
                    pendingPhoto = uri;
                    removePhoto = false;
                    setFormPhoto(bm);
                } catch (Exception e) {
                    UiUtil.toast(this, "Couldn't load that image");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        setupToolbar(b.toolbar, "Contacts", true);
        vm = new ViewModelProvider(this).get(ContactViewModel.class);

        int initial = vm.filter.getValue() == null ? 0 : vm.filter.getValue();
        b.headerContainer.addView(UiUtil.segmented(this, new String[]{"All", "Teachers", "Classmates"}, initial,
                i -> vm.filter.setValue(i)));

        adapter = new ContactAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);
        b.empty.setText("No contacts yet.\nTap + to add a teacher or classmate.");

        vm.visible.observe(this, list -> {
            items.clear();
            items.addAll(list);
            adapter.notifyDataSetChanged();
            b.empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        b.fab.setOnClickListener(v -> showForm(null));

        if (savedInstanceState == null) sync(false);
    }

    // ---------------- sync ----------------
    private void sync(boolean verbose) {
        if (!FirebaseSync.isSignedIn()) {
            if (verbose) UiUtil.toast(this, "Sign in from Settings to sync contacts");
            return;
        }
        ContactSync.syncNow(this, new FirebaseSync.SyncCallback() {
            @Override public void onSuccess() { if (verbose) UiUtil.toast(ContactsActivity.this, "Contacts synced"); }

            @Override public void onError(String error) { if (verbose) UiUtil.toast(ContactsActivity.this, error); }
        });
    }

    // ---------------- list ----------------
    private static void circle(ShapeableImageView iv) {
        iv.setShapeAppearanceModel(iv.getShapeAppearanceModel().toBuilder()
                .setAllCornerSizes(new RelativeCornerSize(0.5f)).build());
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    private static String initialsOf(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] p = name.trim().split("\\s+");
        String s = p[0].substring(0, 1);
        if (p.length > 1) s += p[p.length - 1].substring(0, 1);
        return s.toUpperCase(Locale.getDefault());
    }

    private class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final ItemContactBinding ib;

            VH(ItemContactBinding ib) {
                super(ib.getRoot());
                this.ib = ib;
                circle(ib.avatar);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Contact c = items.get(position);
            h.ib.name.setText(c.name);
            StringBuilder sub = new StringBuilder();
            if (c.rollOrDesignation != null && !c.rollOrDesignation.isEmpty()) sub.append(c.rollOrDesignation);
            if (c.phone != null && !c.phone.isEmpty()) sub.append(sub.length() > 0 ? "\n" : "").append(c.phone);
            if (c.email != null && !c.email.isEmpty()) sub.append(sub.length() > 0 ? "\n" : "").append(c.email);
            h.ib.subtitle.setText(sub);
            boolean teacher = c.type == Contact.TEACHER;
            h.ib.badge.setText(TYPE_NAMES[teacher ? 0 : 1]);
            h.ib.badge.setTextColor(teacher ? 0xFF3F51B5 : 0xFF009688);
            bindAvatar(h, c);
            h.ib.card.setOnClickListener(v -> showActions(c));
            h.ib.card.setOnLongClickListener(v -> { confirmDelete(c); return true; });
        }

        private void showInitials(VH h, Contact c) {
            h.ib.avatar.setVisibility(View.GONE);
            h.ib.initials.setVisibility(View.VISIBLE);
            h.ib.initials.setText(initialsOf(c.name));
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            String n = c.name == null ? "" : c.name;
            g.setColor(UiUtil.COURSE_COLORS[(n.hashCode() & 0x7fffffff) % UiUtil.COURSE_COLORS.length]);
            h.ib.initials.setBackground(g);
        }

        private void bindAvatar(VH h, Contact c) {
            final String path = c.photoPath;
            if (path == null) {
                h.ib.avatar.setTag(null);
                showInitials(h, c);
                return;
            }
            final String key = path + "|" + new File(path).lastModified();
            h.ib.avatar.setTag(key);
            Bitmap cached = photoCache.get(key);
            if (cached != null) {
                h.ib.avatar.setImageBitmap(cached);
                h.ib.avatar.setVisibility(View.VISIBLE);
                h.ib.initials.setVisibility(View.GONE);
                return;
            }
            showInitials(h, c);
            AppExecutors.io(() -> {
                Bitmap bm = ContactPhotos.load(path, 160);
                if (bm == null) return;
                photoCache.put(key, bm);
                AppExecutors.main(() -> {
                    if (!isDestroyed() && key.equals(h.ib.avatar.getTag())) {
                        h.ib.avatar.setImageBitmap(bm);
                        h.ib.avatar.setVisibility(View.VISIBLE);
                        h.ib.initials.setVisibility(View.GONE);
                    }
                });
            });
        }

        @Override
        public int getItemCount() { return items.size(); }
    }

    // ---------------- actions ----------------
    private static boolean filled(String s) { return s != null && !s.isEmpty(); }

    private void launch(Intent i) {
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            UiUtil.toast(this, "No app available for this action");
        }
    }

    private void showActions(Contact c) {
        List<String> labels = new ArrayList<>();
        List<Runnable> acts = new ArrayList<>();
        if (filled(c.phone)) {
            labels.add("Call " + c.phone);
            acts.add(() -> launch(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", c.phone, null))));
            labels.add("Send SMS");
            acts.add(() -> launch(new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", c.phone, null))));
        }
        if (filled(c.email)) {
            labels.add("Email " + c.email);
            acts.add(() -> launch(new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", c.email, null))));
        }
        labels.add("Edit");
        acts.add(() -> showForm(c));
        labels.add("Delete");
        acts.add(() -> confirmDelete(c));
        new MaterialAlertDialogBuilder(this).setTitle(c.name)
                .setItems(labels.toArray(new String[0]), (d, i) -> acts.get(i).run())
                .setNegativeButton("Close", null).show();
    }

    private void confirmDelete(Contact c) {
        UiUtil.confirm(this, "Delete contact?", c.name, "Delete", () -> vm.delete(c));
    }

    // ---------------- add / edit form ----------------
    private void setFormPhoto(Bitmap bm) {
        if (formAvatar == null) return;
        if (bm == null) {
            formAvatar.setImageResource(R.drawable.ic_person);
            int p = UiUtil.dp(this, 18);
            formAvatar.setPadding(p, p, p, p);
        } else {
            formAvatar.setPadding(0, 0, 0, 0);
            formAvatar.setImageBitmap(bm);
        }
    }

    private View photoRow(Contact existing) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        formAvatar = new ShapeableImageView(this);
        circle(formAvatar);
        formAvatar.setBackgroundColor(0x22000000);
        int size = UiUtil.dp(this, 72);
        row.addView(formAvatar, new LinearLayout.LayoutParams(size, size));
        setFormPhoto(existing == null ? null : ContactPhotos.load(existing.photoPath, 256));

        MaterialButton choose = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        choose.setText("Choose photo");
        choose.setAllCaps(false);
        choose.setOnClickListener(v -> photoPicker.launch("image/*"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginStart(UiUtil.dp(this, 12));
        row.addView(choose, lp);

        MaterialButton remove = new MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        remove.setText("Remove");
        remove.setAllCaps(false);
        remove.setOnClickListener(v -> {
            pendingPhoto = null;
            removePhoto = true;
            setFormPhoto(null);
        });
        row.addView(remove);
        return row;
    }

    private void showForm(Contact existing) {
        pendingPhoto = null;
        removePhoto = false;
        FormDialog f = new FormDialog(this);
        int defType = existing != null ? existing.type
                : (vm.filter.getValue() != null && vm.filter.getValue() == 2 ? Contact.CLASSMATE : Contact.TEACHER);
        Spinner type = f.spinner("Type", Arrays.asList(TYPE_NAMES), defType);
        f.addView(photoRow(existing));
        EditText name = f.text("Full name", existing == null ? "" : existing.name);
        EditText role = f.text("Roll no. / Designation", existing == null ? "" : existing.rollOrDesignation);
        EditText phone = f.text("Phone number", existing == null ? "" : existing.phone);
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText email = f.text("Email address", existing == null ? "" : existing.email);
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        f.show(existing == null ? "New contact" : "Edit contact", "Save", () -> {
            String n = FormDialog.str(name), ph = FormDialog.str(phone), em = FormDialog.str(email);
            if (n.isEmpty()) { name.setError("Required"); return false; }
            if (ph.isEmpty() && em.isEmpty()) { phone.setError("Add a phone number or an email"); return false; }
            if (!ph.isEmpty() && !Patterns.PHONE.matcher(ph).matches()) { phone.setError("Invalid number"); return false; }
            if (!em.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(em).matches()) { email.setError("Invalid email"); return false; }
            Contact c = existing == null ? new Contact() : existing;
            c.type = type.getSelectedItemPosition();
            c.name = n;
            c.rollOrDesignation = FormDialog.str(role);
            c.phone = ph;
            c.email = em;
            vm.save(c, pendingPhoto, removePhoto);
            return true;
        });
    }

    // ---------------- menu ----------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, M_SEARCH, Menu.NONE, "Search");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        SearchView sv = new SearchView(this);
        sv.setQueryHint("Search name, roll, phone or email");
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return true; }

            @Override
            public boolean onQueryTextChange(String q) {
                vm.query.setValue(q == null ? "" : q);
                return true;
            }
        });
        item.setActionView(sv);
        addMenu(menu, M_SYNC, "Sync contacts now");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == M_SYNC) {
            sync(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
