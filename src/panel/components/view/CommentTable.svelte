<script>
    import { _ } from "../../../main.js";
    import { Pagination, CardHeader, NoContent } from "@panomc/sdk/components";
    import CommentRow from "../rows/CommentRow.svelte";

    export let comments = [];
    export let commentCount = 0;
    export let page = 1;
    export let totalPage = 1;
    export let onDelete;
    export let onPageClick;
</script>

<div class="card">
    <CardHeader>
        <div slot="left">
            {$_("pages.comments.table-title", {
                values: {
                    commentCount: commentCount || 0,
                },
            })}
        </div>
    </CardHeader>

    <div class="table-responsive">
        <table class="table table-hover align-middle">
            <thead>
                <tr>
                    <th scope="col" class="align-middle" style="width: 40px;"
                    ></th>
                    <th class="align-middle text-nowrap" style="width: 60px;">
                        {$_("pages.comments.table.id")}
                    </th>
                    <th class="align-middle text-nowrap" style="width: 200px;">
                        {$_("pages.comments.table.author")}
                    </th>
                    <th class="align-middle text-nowrap">
                        {$_("pages.comments.table.comment")}
                    </th>
                    <th class="align-middle text-nowrap" style="width: 100px;">
                        {$_("pages.comments.table.likes")}
                    </th>
                    <th class="align-middle text-nowrap" style="width: 150px;">
                        {$_("pages.comments.table.date")}
                    </th>
                </tr>
            </thead>
            <tbody>
                {#if commentCount === 0}
                    <tr>
                        <td colspan="6" class="p-0 border-0">
                            <NoContent
                                text={$_("pages.comments.table.no-comments")}
                                icon="fas fa-comment-slash fa-3x"
                            />
                        </td>
                    </tr>
                {:else}
                    {#each comments as comment (comment.id)}
                        <CommentRow {comment} {onDelete} />
                    {/each}
                {/if}
            </tbody>
        </table>
    </div>

    {#if commentCount > 0}
        <div class="card-footer">
            <Pagination
                {page}
                {totalPage}
                on:firstPageClick={() => onPageClick(1)}
                on:lastPageClick={() => onPageClick(totalPage)}
                on:pageLinkClick={(event) => onPageClick(event.detail.page)}
            />
        </div>
    {/if}
</div>
