const rows = 25;
const cols = ['A', 'B', 'C'];
const grid = document.getElementById('warehouse-grid');

// Initialize Grid
function initGrid() {
    if (!grid) return;
    grid.innerHTML = '';
    for (let r = 1; r <= rows; r++) {
        cols.forEach(c => {
            const id = (r < 10 ? '0' + r : r) + c;
            const cell = document.createElement('div');
            cell.className = 'cell';
            cell.id = 'cell-' + id;
            cell.innerText = id;
            
            const tooltip = document.createElement('div');
            tooltip.className = 'tooltip';
            tooltip.id = 'tooltip-' + id;
            tooltip.innerText = 'Brak danych';
            
            cell.appendChild(tooltip);
            grid.appendChild(cell);
        });
    }
}

// WebSocket Connection
var stompClient = null;

function connect() {
    var socket = new SockJS('/ws-warehouse');
    stompClient = Stomp.over(socket);
    // Disable debug logs
    stompClient.debug = null; 
    
    stompClient.connect({}, function (frame) {
        console.log('Connected to WebSocket');
        stompClient.subscribe('/topic/warehouse/map', function (message) {
            updateMap(JSON.parse(message.body));
        });
    }, function(error) {
        console.error('WebSocket Error:', error);
        setTimeout(connect, 5000);
    });
}

function updateMap(data) {
    const cell = document.getElementById('cell-' + data.locationLabel);
    const tooltip = document.getElementById('tooltip-' + data.locationLabel);
    
    if (cell) {
        // Reset styles first
        cell.style.color = '';
        cell.style.fontWeight = '';

        // Update Color based on Alert/Occupancy
        if (data.alertLevel === 'CRITICAL') {
            cell.style.backgroundColor = 'black';
            cell.style.color = 'red';
            cell.style.fontWeight = 'bold';
        } else if (data.alertLevel === 'WARNING') {
            cell.style.backgroundColor = 'red';
        } else {
            if (data.occupancyPercentage < 20) cell.style.backgroundColor = '#27ae60'; // Green
            else if (data.occupancyPercentage > 80) cell.style.backgroundColor = '#e74c3c'; // Redish
            else cell.style.backgroundColor = '#f39c12'; // Orange
        }

        // Update Tooltip
        if (tooltip) {
            tooltip.innerText = `Stan: ${data.itemsCount} szt. (${data.occupancyPercentage}%)`;
        }
    }
}

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
    initGrid();
    connect();
});
