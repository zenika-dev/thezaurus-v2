import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import type { BlogPostData, BlogPostStatus } from '../../app/types/post';

dayjs.extend(customParseFormat);

const API_URL = 'http://localhost:8080/blog-posts';

function toFrontendStatus(s: string): BlogPostStatus {
  switch (s) {
    case 'DRAFT': return 'Draft';
    case 'PUBLISHED': return 'Published';
    case 'IDEA': return 'Idea';
    case 'REVIEW': return 'Review';
    default: return 'Idea';
  }
}

function toBackendStatus(s: BlogPostStatus): string {
  switch (s) {
    case 'Draft': return 'DRAFT';
    case 'Published': return 'PUBLISHED';
    case 'Idea': return 'IDEA';
    case 'Review': return 'REVIEW';
    default: return 'IDEA';
  }
}

function toFrontendDate(dateStr: string | undefined | null): string {
  if (!dateStr) return '';
  if (/^\d{2}-\d{2}-\d{4}$/.test(dateStr)) return dateStr;
  const parsed = dayjs(dateStr);
  if (parsed.isValid()) return parsed.format('DD-MM-YYYY');
  return dateStr;
}

export function mapBackendToFrontend(p: any): BlogPostData {
  return {
    id: p.id || '',
    title: p.title || '',
    author: p.writers?.[0] || '',
    tags: p.tags || [],
    creationDate: toFrontendDate(p.creationDate),
    expectedPublicationDate: toFrontendDate(p.publicationDate),
    status: toFrontendStatus(p.status),
    zenikaBlogLink: p.link || '',
    googleDocDraftLink: p.link || '',
  };
}

function toLocalDateTime(dateStr: string | undefined): string | null {
  if (!dateStr) return null;
  const ddmmyyyy = dateStr.match(/^(\d{2})-(\d{2})-(\d{4})$/);
  if (ddmmyyyy) {
    return `${ddmmyyyy[3]}-${ddmmyyyy[2]}-${ddmmyyyy[1]}T00:00:00`;
  }
  const yyyymmdd = dateStr.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (yyyymmdd) {
    return `${dateStr}T00:00:00`;
  }
  return dateStr;
}

export function mapFrontendToBackend(p: BlogPostData): any {
  return {
    id: p.id,
    title: p.title,
    writers: [p.author],
    status: toBackendStatus(p.status),
    tags: p.tags,
    creationDate: toLocalDateTime(p.creationDate),
    publicationDate: toLocalDateTime(p.expectedPublicationDate) || null,
    link: p.zenikaBlogLink || '',
  };
}

export const postApi = {
  getPosts: async (): Promise<BlogPostData[]> => {
    const response = await fetch(API_URL);
    if (!response.ok) throw new Error('Failed to fetch posts');
    const data = await response.json();
    return data.map(mapBackendToFrontend);
  },

  createPost: async (post: BlogPostData): Promise<BlogPostData> => {
    const response = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(mapFrontendToBackend(post)),
    });
    if (!response.ok) throw new Error('Failed to create post');
    const data = await response.json();
    return mapBackendToFrontend(data);
  },

  updatePost: async (post: BlogPostData): Promise<BlogPostData> => {
    const response = await fetch(`${API_URL}/${post.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(mapFrontendToBackend(post)),
    });
    if (!response.ok) throw new Error('Failed to update post');
    return post;
  },

  deletePost: async (id: string): Promise<void> => {
    const response = await fetch(`${API_URL}/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to delete post');
  }
};
