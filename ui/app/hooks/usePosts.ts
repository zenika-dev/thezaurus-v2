import { useState, useEffect, useCallback } from 'react';
import { postApi } from '../../src/api/posts.api';
import type { BlogPostData } from '~/types/post';

export function usePosts() {
  const [posts, setPosts] = useState<BlogPostData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchPosts = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await postApi.getPosts();
      setPosts(data);
    } catch (err) {
      console.error('Error fetching posts:', err);
      setError(err instanceof Error ? err : new Error('Unknown error fetching posts'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPosts();
  }, [fetchPosts]);

  const createPost = async (post: BlogPostData) => {
    try {
      const newPost = await postApi.createPost(post);
      setPosts((prev) => [...prev, newPost]);
    } catch (err) {
      console.error('Error creating post:', err);
      throw err;
    }
  };

  const updatePost = async (updatedPost: BlogPostData) => {
    try {
      await postApi.updatePost(updatedPost);
      setPosts((prev) => prev.map((t) => (t.id === updatedPost.id ? updatedPost : t)));
    } catch (err) {
      console.error('Error updating post:', err);
      throw err;
    }
  };

  const deletePost = async (id: string) => {
    try {
      await postApi.deletePost(id);
      setPosts((prev) => prev.filter((t) => t.id !== id));
    } catch (err) {
      console.error('Error deleting post:', err);
      throw err;
    }
  };

  return {
    posts,
    loading,
    error,
    createPost,
    updatePost,
    deletePost,
    refetch: fetchPosts,
  };
}
