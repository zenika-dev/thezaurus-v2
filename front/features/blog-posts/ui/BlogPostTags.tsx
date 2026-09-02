import type { BlogPostStatus } from "@/entities/post";
import { blogPostStatusConfig } from "@/entities/post";
import { Badge } from "@/shared/ui";

export { blogPostStatusConfig as statusConfig };

export function StatusTag({ status }: { status: BlogPostStatus }) {
  const { label, text, bg, darkText, darkBg } = blogPostStatusConfig[status];
  return <Badge color={text} bg={bg} darkColor={darkText} darkBg={darkBg}>{label}</Badge>;
}
