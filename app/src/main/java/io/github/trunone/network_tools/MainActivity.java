package io.github.trunone.network_tools;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        if (savedInstanceState == null) {
            loadFragment(new LocalNetworkFragment(), "Local Network");
            navigationView.setCheckedItem(R.id.nav_local_network);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_local_network) {
            loadFragment(new LocalNetworkFragment(), getString(R.string.local_network));
        } else if (id == R.id.nav_ping) {
            loadFragment(new PingFragment(), getString(R.string.ping));
        } else if (id == R.id.nav_traceroute) {
            loadFragment(new TracerouteFragment(), getString(R.string.traceroute));
        } else if (id == R.id.nav_arp) {
            loadFragment(new ArpFragment(), getString(R.string.arp));
        } else if (id == R.id.nav_dns_lookup) {
            loadFragment(new DnsLookupFragment(), getString(R.string.dns_lookup));
        } else if (id == R.id.nav_port_scanner) {
            loadFragment(new PortScannerFragment(), getString(R.string.port_scanner));
        } else if (id == R.id.nav_subnet_calculator) {
            loadFragment(new SubnetCalculatorFragment(), getString(R.string.subnet_calculator));
        } else if (id == R.id.nav_wake_on_lan) {
            loadFragment(new WakeOnLanFragment(), getString(R.string.wake_on_lan));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

}
