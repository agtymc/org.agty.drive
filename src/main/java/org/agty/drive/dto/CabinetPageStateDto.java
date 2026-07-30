package org.agty.drive.dto;

public class CabinetPageStateDto {

    private Long currentFolderId;
    private String section;
    private String view;
    private String sort;
    private String q;
    private String scope;
    private String shareStatus;
    private String shareType;
    private Long collaborativeAccessId;
    private Integer page;
    private Integer size;

    public Long getCurrentFolderId() {
        return currentFolderId;
    }

    public void setCurrentFolderId(Long currentFolderId) {
        this.currentFolderId = currentFolderId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getShareStatus() {
        return shareStatus;
    }

    public void setShareStatus(String shareStatus) {
        this.shareStatus = shareStatus;
    }

    public String getShareType() {
        return shareType;
    }

    public void setShareType(String shareType) {
        this.shareType = shareType;
    }

    public Long getCollaborativeAccessId() {
        return collaborativeAccessId;
    }

    public void setCollaborativeAccessId(Long collaborativeAccessId) {
        this.collaborativeAccessId = collaborativeAccessId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
