/*
 *    Copyright (C) 2010 - 2015 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.vrem.wifianalyzer.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class WifiInformation {
    private final List<Details> detailsList = new ArrayList<>();
    private final List<Relationship> relationships = new ArrayList<>();

    public WifiInformation() {
    }

    public WifiInformation(List<ScanResult> scanResults, WifiInfo wifiInfo) {
        if (scanResults != null) {
            for (ScanResult scanResult: scanResults) {
                detailsList.add(Details.make(scanResult, getIPAddress(scanResult, wifiInfo)));
            }
            populateRelationship();
            sortRelationship();
        }
    }

    private String getIPAddress(ScanResult scanResult, WifiInfo wifiInfo) {
        if (wifiInfo != null &&
            scanResult.SSID.equals(wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length()-1)) &&
            scanResult.BSSID.equals(wifiInfo.getBSSID())) {

            byte[] bytes = BigInteger.valueOf(wifiInfo.getIpAddress()).toByteArray();
            ArrayUtils.reverse(bytes);
            try {
                return InetAddress.getByAddress(bytes).getHostAddress();
            } catch (UnknownHostException e) {
                Log.e("IPAddress", e.getMessage());
            }
        }
        return "";
    }

    private void populateRelationship() {
        Collections.sort(detailsList, new SSIDComparator());
        Relationship relationship = null;
        for (Details details: this.detailsList) {
            if (relationship == null || !relationship.parent.getSSID().equals(details.getSSID())) {
                relationship = new Relationship(details);
                relationships.add(relationship);
            } else {
                relationship.chidlren.add(details);
            }
        }
    }

    private void sortRelationship() {
        Collections.sort(this.relationships);
        for (Relationship information: this.relationships) {
            Collections.sort(information.chidlren, new LevelComparator());
        }
    }

    public int getParentsSize() {
        return this.relationships.size();
    }
    public Details getParent(int index) {
        return this.relationships.get(index).parent;
    }

    public int getChildrenSize(int index) {
        return this.relationships.get(index).chidlren.size();
    }

    public Details getChild(int indexParent, int indexChild) {
        return this.relationships.get(indexParent).chidlren.get(indexChild);
    }

    class SSIDComparator implements Comparator<Details> {
        @Override
        public int compare(Details lhs, Details rhs) {
            int result = lhs.getSSID().compareTo(rhs.getSSID());
            if (result == 0) {
                result = lhs.getLevel() - rhs.getLevel();
                if (result == 0) {
                    result = lhs.getBSSID().compareTo(rhs.getBSSID());
                }
            }
            return result;
        }
    }

    class LevelComparator implements Comparator<Details> {
        @Override
        public int compare(Details lhs, Details rhs) {
            int result = lhs.getLevel() - rhs.getLevel();
            if (result == 0) {
                result = lhs.getSSID().compareTo(rhs.getSSID());
                if (result == 0) {
                    result = lhs.getBSSID().compareTo(rhs.getBSSID());
                }
            }
            return result;
        }
    }

    class Relationship implements Comparable<Relationship> {
        public final Details parent;
        public final List<Details> chidlren = new ArrayList<>();

        public Relationship(@NonNull Details parent) {
            this.parent = parent;
        }

        @Override
        public int compareTo(@NonNull Relationship other) {
            int result = this.parent.getLevel() - other.parent.getLevel();
            if (result == 0) {
                result = this.parent.getSSID().compareTo(other.parent.getSSID());
                if (result == 0) {
                    result = this.parent.getBSSID().compareTo(other.parent.getBSSID());
                }
            }
            return result;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(detailsList, ((WifiInformation) o).detailsList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detailsList);
    }
}