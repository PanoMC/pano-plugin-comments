<script>
    import { page } from "@panomc/sdk/svelte";
    export let post;
    $: count = $page.data.commentCounts?.[post.id] ?? 0;
</script>

<span class="badge bg-light text-dark border">
    {count}
</span>
