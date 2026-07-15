import { useEffect, useMemo, useState } from 'react';

function sortVersions(versions, sortBy, sortOrder) {
  return [...versions].sort((a, b) => {
    let comparison = 0;
    if (sortBy === 'versionNumber') comparison = a.versionNumber - b.versionNumber;
    else if (sortBy === 'createdAt') comparison = new Date(a.createdAt) - new Date(b.createdAt);
    else if (sortBy === 'status') {
      const statusOrder = { ARCHIVED: 1, DRAFT: 2, PUBLISHED: 3 };
      comparison = statusOrder[a.status] - statusOrder[b.status];
    }
    return sortOrder === 'asc' ? comparison : -comparison;
  });
}

export function useVersionListControls(versions) {
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [sortBy, setSortBy] = useState('versionNumber');
  const [sortOrder, setSortOrder] = useState('desc');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [versionSearch, setVersionSearch] = useState('');

  const filteredVersions = useMemo(() => {
    let filtered = [...versions];
    if (statusFilter !== 'ALL') filtered = filtered.filter((v) => v.status === statusFilter);
    if (versionSearch.trim()) {
      const searchTerm = versionSearch.trim();
      filtered = filtered.filter((v) => v.versionNumber.toString() === searchTerm);
    }
    return sortVersions(filtered, sortBy, sortOrder);
  }, [sortBy, sortOrder, statusFilter, versionSearch, versions]);

  const totalPages = Math.ceil(filteredVersions.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedVersions = filteredVersions.slice(startIndex, endIndex);

  useEffect(() => {
    setCurrentPage(1);
  }, [statusFilter, versionSearch, sortBy, sortOrder, itemsPerPage]);

  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) setCurrentPage(newPage);
  };

  const renderPageNumbers = () => {
    const pages = [];
    const maxPagesToShow = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
    let endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);
    if (endPage - startPage < maxPagesToShow - 1) startPage = Math.max(1, endPage - maxPagesToShow + 1);
    for (let i = startPage; i <= endPage; i++) {
      pages.push(<button key={i} onClick={() => handlePageChange(i)} className={`page-number ${i === currentPage ? 'active' : ''}`}>{i}</button>);
    }
    return pages;
  };

  return {
    controls: { versionSearch, setVersionSearch, statusFilter, setStatusFilter, sortBy, setSortBy, sortOrder, setSortOrder, itemsPerPage, setItemsPerPage },
    filteredVersions,
    paginatedVersions,
    pagination: { totalPages, currentPage, startIndex, endIndex, handlePageChange, renderPageNumbers },
  };
}
