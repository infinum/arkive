// Fetch the JSON file and populate the grid
document.addEventListener('DOMContentLoaded', () => {
    const grid = document.getElementById('grid');
    
    fetch('arkive-showcase.json') // Assumes the JSON file is named 'components.json'
      .then(response => response.json())
      .then(data => {
        const items = data.items || [];
        
        items.forEach(item => {
          const { component, snapshotPath } = item;
          const gridItem = document.createElement('div');
          gridItem.className = 'grid-item';
  
          gridItem.innerHTML = `
            <img src="${snapshotPath}" alt="${component.name}" />
            <div class="name">${component.name}</div>
          `;
  
          grid.appendChild(gridItem);
        });
      })
      .catch(error => console.error('Error loading JSON:', error));
  });