// TODO MAPPER FOR BLOG POSTS

import type { BlogPostData } from '../../app/types/post';

const API_URL = 'http://localhost:8080/blog-posts';

export function mapBackendToFrontend(p: any): BlogPostData {
  const speakers = p.speakers || [];
  return {
    id: p.id || '',
    title: p.title || '',
    author: p.writers[0] || '',
    tags: p.tags || [],
    creationDate: p.creationDate || '',
    expectedPublicationDate: p.publicationDate,
    status: p.status === 'DRAFT' ? 'Draft' : 'Published',
    zenikaBlogLink: p.link || '',
    googleDocDraftLink: p.link || '',
  };
}

export function mapFrontendToBackend(p: BlogPostData): any {
//   const writers = [];
//   if (p.speaker.trim()) speakers.push(p.speaker.trim());
//   if (p.cospeaker.trim()) speakers.push(p.cospeaker.trim());

  let backendStatus = 'DRAFT';
if (p.status === 'Published') backendStatus = 'PUBLISHED';

  return {
    id: p.id,
    title: p.title,
    writers: p.author,
    creationDate: p.creationDate,
    publicationDate: p.expectedPublicationDate || '',
    status: backendStatus,
    link: p.zenikaBlogLink || '',
    // add googleDocDraftLink
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
